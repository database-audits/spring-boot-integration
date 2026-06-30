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

// For each name in dataSourceNames (comma-separated; 'none' or empty means none), generate a
// DatabaseAudit<Name>TestConfiguration plus a DatabaseAudit<Name>IT from the multi/ token templates, then delete
// the templates; with no names, remove the whole multi/ directory.
def dataSourceNames = request.properties.getProperty('dataSourceNames', 'none')
def multiDir = new File(destRoot, "src/test/java/${packagePath}/multi")
def names = []
if (dataSourceNames && dataSourceNames != 'none') {
    names = dataSourceNames.split(',').collect { it.trim() }.findAll { it }
    names.each { name ->
        if (!(name ==~ /[A-Za-z_][A-Za-z0-9_]*/)) {
            throw new IllegalArgumentException("Invalid dataSourceNames entry '" + name +
                    "': each name must be a valid Java identifier (a letter or underscore followed by " +
                    "letters, digits, or underscores) so it can form class names like " +
                    "DatabaseAudit<Name>TestConfiguration.")
        }
    }
}
if (names) {
    def configTemplate = new File(multiDir, "DatabaseAuditDsNameTokenTestConfiguration.java")
    def itTemplate = new File(multiDir, "DatabaseAuditDsNameTokenIT.java")
    def configBody = configTemplate.getText('UTF-8')
    def itBody = itTemplate.getText('UTF-8')
    names.each { name ->
        def pascal = name.substring(0, 1).toUpperCase() + name.substring(1)
        def camel = name.substring(0, 1).toLowerCase() + name.substring(1)
        new File(multiDir, "DatabaseAudit${pascal}TestConfiguration.java")
                .write(configBody.replace('DsNameToken', pascal).replace('dsNameToken', camel), 'UTF-8')
        new File(multiDir, "DatabaseAudit${pascal}IT.java")
                .write(itBody.replace('DsNameToken', pascal).replace('dsNameToken', camel), 'UTF-8')
    }
    configTemplate.delete()
    itTemplate.delete()
} else {
    multiDir.deleteDir()
}
