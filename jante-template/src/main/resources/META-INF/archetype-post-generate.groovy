import java.nio.file.Files

File moduleDir = new File(request.getOutputDirectory() + "/" + request.getArtifactId())

File pomFile = new File(moduleDir, 'pom.xml')

List<String> pomContents = Files.readAllLines(pomFile.toPath())
boolean includeStatePom = true
String newPomContent = ""
for (String line : pomContents) {

    if (line.contains('__MARKER_START__')) {
        includeStatePom = false;
    }
    if (includeStatePom) {
        System.out.println(line);
        newPomContent += line + '\n';
    }
    if (line.contains('__MARKER_END__')) {
        includeStatePom = true;
    }

}

pomFile.newWriter().withWriter { w ->
    w << newPomContent
}

File gitIgnoreFile = new File(moduleDir, 'archetype_gitignore')
boolean renamed = gitIgnoreFile.renameTo(new File(moduleDir, '.gitignore'))
if (!renamed) {
    throw new RuntimeException("Could not rename gitignore file")
}


File readmeFile = new File(moduleDir, 'README.md')

List<String> readmeContents = Files.readAllLines(readmeFile.toPath())
boolean includeStateReadme = true
String newReadmeContent = ""
for (String line : readmeContents) {

    if (line.contains('__MARKER_START__')) {
        includeStateReadme = false;
    }
    if (includeStateReadme) {
        System.out.println(line);
        newReadmeContent += line + '\n';
    }
    if (line.contains('__MARKER_END__')) {
        includeStateReadme = true;
    }

}

readmeFile.newWriter().withWriter { w ->
    w << newReadmeContent
}
