package edu.msudenver.cs.jdnss;

import lombok.Getter;

enum JDNSSLogLevels {OFF, FATAL, ERROR, WARN, INFO, DEBUG, TRACE, ALL}

class jdnssArgs {
    @Getter private boolean once = false;
    @Getter private int threads = 10;

    @Getter private boolean version;
    String[] IPaddresses = {"TLS@0.0.0.0@853", "TCP@0.0.0.0@53", "UDP@0.0.0.0@53"}; // "MC@224.0.0.251@5353"
    int backlog = 4;
    JDNSSLogLevels logLevel = JDNSSLogLevels.ERROR;

    @Getter private boolean help;

    @Getter private String DBClass;
    @Getter private String DBURL;
    @Getter private String DBUser;
    @Getter private String DBPass;

    String serverSecret;

    String keystoreFile;
    String keystorePassword;
    boolean debugSSL;

    @Getter private String additional[];
}

/*

Reciprocity
Consistency
Social Proof
Authority
Scarcity

Robert B. Cialdini in his book Influence: The Psychology of Persuasion identifies principles of persuasion we can use to understand why we're easy to manipulate.
*/

/*
docker run --rm --interactive --tty debian:latest /bin/bash

client.containers.run(image='debian:latest', command='/bin/bash', auto_remove=True, tty=True)

client=docker.APIClient()

# make sure it's been pulled

container = client.create_container(
    image='debian:latest',
    stdin_open=True,
    tty=True,
    command='/bin/bash',
)

dockerpty.start(client, container)

*/
