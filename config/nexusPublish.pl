#!/usr/bin/perl -w

## Configure these
use constant REPO_PATH => "/home/asx/UploadToNexus/StandaloneRepo-6.2.1-FINAL";
use constant NEXUS_URL => "http://172.17.23.63:8081";
use constant REPO => "sabre";
use constant NEXUS_ADMIN_USER => "admin";
use constant NEXUS_ADMIN_PASSWORD => "admin";

## No need to configure below this line
my $RELEASE_BASE_URL=NEXUS_URL . "/nexus/content/repositories/" . REPO . "-release";
my $SNAPSHOT_BASE_URL=NEXUS_URL . "/nexus/content/repositories/" . REPO . "-snapshot";

my $cmd="find " . REPO_PATH . " -name \*.pom";
my @POM_LIST=`$cmd`;
print "Discovered " . $#POM_LIST . " pom files\n";
foreach(@POM_LIST) {
    my $POM = $_;
    chop($POM);
    $POM = substr($POM, length(REPO_PATH)+1);

    print "RAW POM = $POM\n";
    my @arr = split("/", $POM);
    my $POM_FILE = pop(@arr);
    my $JAR_FILE = substr($POM_FILE,0, length($POM_FILE)-4) . ".jar";
    my $SOURCES_JAR_FILE = substr($POM_FILE,0, length($POM_FILE)-4) . "-sources.jar";
    print "POM FILE             = $POM_FILE\n";
    print "JAR FILE             = $JAR_FILE\n";
    print "SOURCES JAR FILE     = $SOURCES_JAR_FILE\n";
    my $ARTIFACT = join("/", @arr);
    print "ARTIFACT = $ARTIFACT\n";

    ## Determine if this is
    my $URL = ($ARTIFACT =~ m/SNAPSHOT/) ? $SNAPSHOT_BASE_URL : $RELEASE_BASE_URL;

    ## Upload POM
    my $curl = "curl -u " . NEXUS_ADMIN_USER . ":" . NEXUS_ADMIN_PASSWORD . " --upload-file " . REPO_PATH . "/" . $ARTIFACT . "/$POM_FILE $URL/$ARTIFACT/$POM_FILE";
    print "EXEC: $curl \n";
    system($curl);

    ## Upload JAR
    $curl = "curl -u " . NEXUS_ADMIN_USER . ":" . NEXUS_ADMIN_PASSWORD . " --upload-file " . REPO_PATH . "/" . $ARTIFACT . "/$JAR_FILE $URL/$ARTIFACT/$JAR_FILE";
    print "EXEC: $curl \n";
    system($curl);

    ## Upload SOURCES JAR
    $curl = "curl -u " . NEXUS_ADMIN_USER . ":" . NEXUS_ADMIN_PASSWORD . " --upload-file " . REPO_PATH . "/" . $ARTIFACT . "/$SOURCES_JAR_FILE $URL/$ARTIFACT/$SOURCES_JAR_FILE";
    print "EXEC: $curl \n";
    system($curl);
}
