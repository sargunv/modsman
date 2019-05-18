package modsman

sealed class ModsmanException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class ChooseFileException(version: String) :
    ModsmanException("Failed to find a valid version matching '$version'")

class UpgradeException(mod: ModEntry, cause: ChooseFileException) :
    ModsmanException("Failed to upgrade '${mod.projectName}', caused by: ${cause.message}", cause)

class InstallException(name: String, cause: ChooseFileException) :
    ModsmanException("Failed to install '$name', caused by: ${cause.message}", cause)

class ProjectNotFoundException(projectId: Int) :
    ModsmanException("Project '$projectId' not found")
