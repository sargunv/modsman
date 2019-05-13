module modsman.cli {
    requires modsman.core;
    requires kotlin.stdlib;
    requires kotlin.stdlib.jdk8;
    requires kotlinx.coroutines.core;
    requires jcommander;

    exports dev.sargunv.modsman.cli;
}
