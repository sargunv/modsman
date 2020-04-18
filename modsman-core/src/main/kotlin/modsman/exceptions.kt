package modsman

sealed class ModsmanException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

class DownloadException(url: String, fileName: String, cause: Throwable) :
    ModsmanException(
        "Failed to download '$fileName' from '$url'"
            + ", caused by ${cause::class.java.simpleName}: ${cause.message}",
        cause
    )

class PinnedException(mod: ModEntry) :
    ModsmanException("Won't upgrade pinned mod '${mod.projectName}'")

class ChooseFileException(config: ModlistConfig) :
    ModsmanException(
        "Failed to find a valid version matching ${config.requiredGameVersions} "
            + "and excluding ${config.excludedGameVersions}"
    )

class UpgradeException(mod: ModEntry, cause: ChooseFileException) :
    ModsmanException("Failed to upgrade '${mod.projectName}', caused by: ${cause.message}", cause)

class InstallException(name: String, cause: ChooseFileException) :
    ModsmanException("Failed to install '$name', caused by: ${cause.message}", cause)

class ProjectNotFoundException(projectId: Int) :
    ModsmanException("Project '$projectId' not found")
