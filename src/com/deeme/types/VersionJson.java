package com.deeme.types;

public class VersionJson {
    private String versionNumber;
    private String downloadLink;
    private String changelog;
    private String minVersion;

    public String getDownloadLink() { return this.downloadLink; }

    public String getVersionNumber() { return this.versionNumber; }

    public String getChangelog() { return this.changelog; }

    public String getMinVersion() { return this.minVersion; }
}

