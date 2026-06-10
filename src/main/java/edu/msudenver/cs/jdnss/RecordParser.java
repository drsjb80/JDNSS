package edu.msudenver.cs.jdnss;

interface RecordParser {
    void parse(Parser parser, ParsingContext context) throws Exception;
}
