SUMMARY = "LHR Solar McQueen CAN DBC database files"
DESCRIPTION = "CAN bus DBC database files describing inter-board messaging on \
the McQueen vehicle. Pulled from lhr-solar/Embedded-Sharepoint."
LICENSE = "CLOSED"

# Fetch the Mcqueen DBC set from the Embedded-Sharepoint mono-repo.
# AUTOREV tracks HEAD of main so every build picks up the latest DBC
# definitions. For release reproducibility, pin to a specific SHA, e.g.:
#   SRCREV = "260c8f9426f3be10e58db60574779992d7b38411"
SRC_URI = "git://github.com/lhr-solar/Embedded-Sharepoint.git;protocol=https;branch=main"
SRCREV = "${AUTOREV}"

PV = "0.1+git${SRCPV}"
S = "${WORKDIR}/git"

# DBC files are plain-text, architecture-independent. No compile step.
inherit allarch

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}${datadir}/can-dbc/Mcqueen
    install -m 0644 ${S}/can/dbc/Mcqueen/*.dbc ${D}${datadir}/can-dbc/Mcqueen/
    # McQueen.mdc is a Vector metadata companion to the DBC set — include it
    # if the upstream still ships it.
    if [ -f ${S}/can/dbc/Mcqueen/McQueen.mdc ]; then
        install -m 0644 ${S}/can/dbc/Mcqueen/McQueen.mdc \
            ${D}${datadir}/can-dbc/Mcqueen/
    fi
}

FILES:${PN} = "${datadir}/can-dbc"
