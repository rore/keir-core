"""Resolve Maven in the project cache for release preparation and smoke tests."""

import os
import re


def maven_command(source, maven_repo, offline):
    maven_home = maven_repo.parent
    env = {**os.environ, "MAVEN_USER_HOME": str(maven_home)}
    windows = os.name == "nt"
    maven = source / ("mvnw.cmd" if windows else "mvnw")
    if offline:
        # Maven's offline flag does not stop the wrapper from downloading Maven itself.
        properties = (source / ".mvn/wrapper/maven-wrapper.properties").read_text(encoding="utf-8")
        distribution = re.search(r"^distributionUrl=.*?/(apache-maven-[\w.\-]+)-bin\.zip$", properties, re.M)
        if not distribution:
            raise ValueError("Expected an Apache Maven ZIP in the wrapper properties")
        binary = "mvn.cmd" if windows else "mvn"
        cached = sorted((maven_home / "wrapper/dists" / distribution[1]).glob(f"*/bin/{binary}"))
        if len(cached) != 1:
            raise ValueError("Offline release requires one cached copy of the pinned Maven distribution")
        maven = cached[0]
    elif not windows:
        maven.chmod(0o755)
    command = [str(maven), "-B", "-ntp", f"-Dmaven.repo.local={maven_repo}"]
    if offline:
        command.append("-o")
    return command, env
