/**
 * Simple script to extract query information to csv for use in load-testing (e.g. JMeter)
 * http://opencsv.sourceforge.net/ for writing 'safe' csv
 *
 * alternative: perl -lne 'print $1 if /foobar (\w+)/' < test.txt   https://unix.stackexchange.com/questions/13466/can-grep-output-only-specified-groupings-that-match
 */
import com.opencsv.CSVWriter
import groovy.transform.Field
import org.apache.log4j.Logger

import java.text.SimpleDateFormat
import java.util.regex.Matcher
import java.util.regex.Pattern

@Field
final Logger log = Logger.getLogger(this.class.name);
log.info "Starting ${this.class.name}..."

// https://regex101.com/r/kqB0NT/1/
// 2020-12-11 03:17:00.803 INFO  (qtp2048537720-14021) [c:test-lw s:shard1 r:core_node3 x:test-lw_shard1_replica_n1] o.a.s.c.S.Request [test-lw_shard1_replica_n1]  webapp=/solr path=/select params={df=_text_&distrib=false&hl=false&fl=id&fl=score&shards.purpose=16388&fsv=true&shard.url=http://shared-solr-0.shared-solr-headless:8983/solr/test-lw_shard1_replica_n1/&_forwardedCount=1&rid=shared-solr-0.shared-solr-headless-test-lw_shard1_replica_n1-1607656620792-3393&defType=edismax&context=app:nmworkplace&shards.preference=replica.type:PULL&wt=javabin&q.alt=*:*&debug=false&debug=timing&debug=track&json.nl=arrarr&lw.pipelineId=_lw_tmp_1607648073&start=0&isFusionQuery=true&rows=10&fusionQueryId=V70y8X1SDl&version=2&hl.snippets=3&q=*:*&omitHeader=false&requestPurpose=GET_TOP_IDS,SET_TERM_STATS&NOW=1607656620791&isShard=true&debugQuery=false&username=MAL9793-NMTEST@nmtest.nmfco.com} hits=284 status=0 QTime=0
// groups: 1=collection, 2=all params (shift to end of output for visual preference), 3=query, 4=hits, 5=status, 6-QTime
// ADJUST as desired
Pattern queryPartsRegex = Pattern.compile(/^(.*) INFO *\([^)]+\) \[c:(.*) s:shard.* params=\{(.*\bq=([^&]+).*)\} hits=(\d+) status=(\d+) QTime=(\d+)/)

// TODO -- write header line
String[] headers = ['timestamp', 'collection', 'query', 'hits', 'status', 'QTime', 'allParams', 'distrib', 'userQuery', 'logFile', 'lineNo']

// TODO allow to override with args
// TODO consider SSH/SFTP/SSHFS to read remote logs...?
// currently just storing log files in the resources folder, but override this with a solr logs folder as appropriate
File aResourceFile = new File(this.getClass().getClassLoader().getResource("log4j.properties").toURI())
File sourceLogFileDir = aResourceFile.parentFile                                // get parent "resources" folder, where by default we are saving solr log files to be analyzed
log.info "Using source log file dir: $sourceLogFileDir"

Date currentTime = new Date()
SimpleDateFormat sdf = new SimpleDateFormat('yyyyMMdd.hhmm')            // ADJUST as desired: allowing for 'versioning' of runs
String datePart = sdf.format(currentTime)
File outputCsv = new File(sourceLogFileDir.parentFile.parentFile.parentFile,"output/gseq-${datePart}.csv")
log.info "Using output file as: $outputCsv"

//char seperator = ',' as char
CSVWriter csvWriter = new CSVWriter(new FileWriter(outputCsv));                 // TODO -- problem with groovy magic and getting an actual char for params, just go with defaults...
csvWriter.writeNext(headers)

int logFileCount = 0
Pattern solrLogFileRegex = ~/.*solr.*logs?/                                     // ADJUST as desired
sourceLogFileDir.eachFileMatch(solrLogFileRegex) { File solrLog ->
    logFileCount++
    log.info "$logFileCount) Processing file: $solrLog..."
    int idx = 0
    solrLog.eachLine { String line ->
        idx++
        log.debug "\t$idx) Line: ${line}"
        String[] entries
        parseQueryLineWithRegex(line, queryPartsRegex, solrLog, idx, csvWriter)
    }           // end each line
}



private List<String> parseQueryLineWithRegex(String line, Pattern queryPartsRegex, File solrLog, int idx) {
    List entries
    Matcher m = line =~ queryPartsRegex
    if (m.matches()) {
        List<String> groups = m[0]
        String timeStamp = groups[1]
        String collection = groups[2]
        String allParams = groups[3]
        String q = groups[4]
        // TODO get filter queries as well

        String hits = groups[5]
        String status = groups[6]
        String qtime = groups[7]

        String isuserqry = isUserQueryLine(line)
        String isdistrib = isDistributed(allParams)

        entries = [timeStamp, collection, q, hits, status, qtime, allParams, isdistrib, isuserqry, solrLog, idx]
        log.debug "\t\t$idx) adding qry for line: $line"

    } else {
        log.debug "\t\t\not a user query..."
    }
    return entries
}

log.info "writing output file: $outputCsv..."
csvWriter.close()


/**
 * generalize the output process
 * @param outputLine
 * @param outFile
 * @return
 */
def addOutputLine(String[] entries, CSVWriter writer, int i) {
    log.info "\t\t$i) adding output entries: $entries"
    writer.writeNext(entries)
}


/**
 * try to distinguish user query from system queries
 * @param line
 * @return
 */
boolean isUserQueryLine(String line) {
    if (line.contains("&q=")) {
        return true
    } else {
        return false
    }
}

/**
 * determin if this is a distributed query (in which case you probably want to exclude it for load testing, and just use the "original" non-distrib version)
 * TODO revisit logic on to/from leader etc
 * @param allParams
 * @return
 */
boolean isDistributed(String allParams) {
    if (allParams.toLowerCase().contains("distrib=toleader") || allParams.toLowerCase().contains("distrib=fromleader")) {
        return true
    } else {
        return false
    }
}

