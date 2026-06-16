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

// A relative projectDirectory resolves against the shell's working directory. Prefer the PWD
// environment variable: the shell exports it and, unlike user.dir, it cannot be mutated by a
// System.setProperty call inside the running JVM. Fall back to user.dir when PWD is absent (e.g. on
// Windows). Tests inject _pwd through the Groovy Binding to stand in for PWD.
def pwdOverride = binding.hasVariable('_pwd') ? (String) binding.getVariable('_pwd') : null
def workingDir = pwdOverride ?: System.getenv('PWD') ?: System.getProperty('user.dir')

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
