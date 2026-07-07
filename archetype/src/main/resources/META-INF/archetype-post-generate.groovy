def generateMode = request.properties.getProperty('generateMode', 'project')
def parentClass = request.properties.getProperty('parentClass', 'none')
def outputDir = new File(request.outputDirectory, request.artifactId)
def packagePath = request.properties.getProperty('package').replace('.', '/')

// projectDirectory is not a declared archetype property, so archetype:generate does not place it in
// request.properties — on the command line it arrives only as a JVM system property
// (-DprojectDirectory=...). The archetype integration-test mojo, by contrast, supplies it through
// archetype.properties, where it does land in request.properties. Read request.properties first
// (the IT path), then fall back to the system property (the command-line path).
def targetDirProp = request.properties.getProperty('projectDirectory')
if (!targetDirProp) {
    targetDirProp = System.getProperty('projectDirectory')
}

// A relative projectDirectory resolves against the shell's working directory, taken from the PWD
// environment variable (the shell exports it and, unlike user.dir, it cannot be mutated by a
// System.setProperty call inside the running JVM; tests inject _pwd through the Groovy Binding to
// stand in for it). Under Git Bash on Windows, though, PWD is a Unix-style path (e.g. /c/Users/...)
// that java.io.File resolves to a bogus drive-relative path, so trust PWD only when it names an
// existing directory on this platform; otherwise fall back to user.dir (the real JVM working dir).
def pwd = binding.hasVariable('_pwd') ? (String) binding.getVariable('_pwd') : System.getenv('PWD')
def workingDir = (pwd && new File(pwd).isDirectory()) ? pwd : System.getProperty('user.dir')

def resolveDestRoot = { prop ->
    if (!prop) {
        return new File(request.outputDirectory)
    }
    def f = new File(prop)
    return f.absolute ? f : new File(workingDir, prop)
}

if (generateMode != 'project') {
    [
        "src/test/java/${packagePath}/DemoApplication.java",
        "src/test/java/${packagePath}/app",
        "src/test/java/${packagePath}/runtime/RepositoryWorkloadIT.java",
        "src/test/resources/application.properties",
        "src/test/resources/db"
    ].each { path ->
        def f = new File(outputDir, path)
        if (f.isDirectory()) { f.deleteDir() } else { f.delete() }
    }
    new File(outputDir, "pom.xml").delete()

    // Move src/ to the project root. projectDirectory lets the user point outputDirectory at a scratch
    // location (bypassing the archetype plugin's pom.xml check) while still writing into their real project.
    def destRoot = resolveDestRoot(targetDirProp)
    def srcDir = new File(outputDir, "src")
    srcDir.eachFileRecurse { f ->
        if (f.isFile()) {
            def rel = srcDir.toPath().relativize(f.toPath()).toString()
            def dest = new File(destRoot, "src" + File.separator + rel)
            dest.parentFile?.mkdirs()
            dest.bytes = f.bytes
        }
    }
    srcDir.deleteDir()
    outputDir.deleteDir()
}

def destRoot = (generateMode == 'project') ? outputDir : resolveDestRoot(targetDirProp)
if (parentClass != 'none') {
    new File(destRoot, "src/test/java/${packagePath}/AbstractDatabaseAuditIT.java").delete()
}

// The plan-based runtime audits (Join/OrderBy/Unused/WhereClause index) are PostgreSQL-only, so remove their ITs
// for any other engine. The catalog, JPA, and capture-scan (offset-pagination/repeated-statement/
// unconditional-mutation) audits run on every engine; RepositoryWorkloadIT stays too, because the
// unconditional-mutation audit throws on an empty capture, so its priming workload must still run.
def databasePlatform = request.properties.getProperty('databasePlatform', 'postgresql')
if (databasePlatform != 'postgresql') {
    [
        "src/test/java/${packagePath}/runtime/JoinIndexAuditIT.java",
        "src/test/java/${packagePath}/runtime/OrderByIndexAuditIT.java",
        "src/test/java/${packagePath}/runtime/UnusedIndexAuditIT.java",
        "src/test/java/${packagePath}/runtime/WhereClauseIndexAuditIT.java"
    ].each { path ->
        new File(destRoot, path).delete()
    }
}

// When dataSourceName is set (not 'none'), the application configures several peer datasources with no @Primary:
// expand the token config template into a DatabaseAudit<Name>TestConfiguration that resolves this datasource's
// beans by @Qualifier (each audit IT @Imports it), then delete the token template. With no dataSourceName, just
// remove the token template — the ITs import the stock DatabaseAuditTestConfiguration instead.
def dataSourceName = request.properties.getProperty('dataSourceName', 'none')
def configTemplate = new File(destRoot, "src/test/java/${packagePath}/DatabaseAuditDsNameTokenTestConfiguration.java")
if (dataSourceName && dataSourceName != 'none') {
    if (!(dataSourceName ==~ /[A-Za-z_][A-Za-z0-9_]*/)) {
        throw new IllegalArgumentException("Invalid dataSourceName '" + dataSourceName +
                "': it must be a valid Java identifier (a letter or underscore followed by letters, digits, or " +
                "underscores) so it can form the class name DatabaseAudit<Name>TestConfiguration.")
    }
    def dataSourceBeanName = request.properties.getProperty('dataSourceBeanName', 'none')
    def entityManagerFactoryBeanName = request.properties.getProperty('entityManagerFactoryBeanName', 'none')
    if (!dataSourceBeanName || dataSourceBeanName == 'none'
            || !entityManagerFactoryBeanName || entityManagerFactoryBeanName == 'none') {
        throw new IllegalArgumentException("dataSourceName '" + dataSourceName +
                "' requires both dataSourceBeanName and entityManagerFactoryBeanName, naming the DataSource and " +
                "EntityManagerFactory beans the generated config resolves by @Qualifier.")
    }
    def pascal = dataSourceName.substring(0, 1).toUpperCase() + dataSourceName.substring(1)
    def camel = dataSourceName.substring(0, 1).toLowerCase() + dataSourceName.substring(1)
    def configBody = configTemplate.getText('UTF-8')
    new File(configTemplate.parentFile, "DatabaseAudit${pascal}TestConfiguration.java")
            .write(configBody.replace('DsNameToken', pascal).replace('dsNameToken', camel), 'UTF-8')
    configTemplate.delete()
} else {
    configTemplate.delete()
}

// Remaining manual steps for the consumer, printed to the archetype:generate console output. The audit ITs no
// longer carry @Import themselves; the config is imported once on the base class (generated or user-specified).
def schemaProp = request.properties.getProperty('schemaPropertyName', 'database.datasource.schema-name')
def site = 'https://database-audits.github.io/spring-boot-integration'
def reportFormat = request.properties.getProperty('reportFormat', 'asciidoc')
def fixFormat = request.properties.getProperty('fixFormat', 'liquibase-xml')
def reportExt = ['markdown': 'md', 'asciidoc': 'adoc', 'text': 'txt'].getOrDefault(reportFormat, 'adoc')
println ''
println '=================================================================================='
println ' database-audits: generation complete. Remaining manual steps:'
println '=================================================================================='
if (dataSourceName && dataSourceName != 'none') {
    def pascal = dataSourceName.substring(0, 1).toUpperCase() + dataSourceName.substring(1)
    def configClass = "DatabaseAudit${pascal}TestConfiguration"
    if (parentClass && parentClass != 'none') {
        println " * Add @Import(${configClass}.class) to your base test class ${parentClass}"
        println "   (or list ${configClass}.class in its @ContextConfiguration(classes = { ... }))."
    }
    if (databasePlatform == 'postgresql') {
        println " * Add preferQueryMode=simple to the ${dataSourceName} datasource's JDBC URL (plan-based PostgreSQL"
        println "   runtime audits only)."
    }
    println " * Guide: ${site}/usage.html#_multiple_datasources"
} else if (parentClass && parentClass != 'none') {
    println " * Add @Import(DatabaseAuditTestConfiguration.class) to your base test class ${parentClass}"
    println "   (or list it in its @ContextConfiguration(classes = { ... }))."
    println " * Guide: ${site}/usage.html"
} else {
    println " * Guide: ${site}/usage.html"
}
println " * Ensure ${schemaProp} is set to the schema the catalog audits scan."
if (fixFormat != 'none') {
    println " * When a run has findings, a consolidated report (with ${fixFormat} fix suggestions) is written to"
    println "   target/database-audit-report.${reportExt}; tune database-audits.report.* in junit-platform.properties."
} else {
    println " * When a run has findings, a consolidated report is written to target/database-audit-report.${reportExt};"
    println "   set database-audits.report.fix-format=sql|liquibase-xml in junit-platform.properties to also emit fixes."
}
println '=================================================================================='
