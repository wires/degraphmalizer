import com.tinkerpop.blueprints.Direction
import com.fasterxml.jackson.databind.ObjectMapper;

objectMapper = new ObjectMapper()

def line() { println ("-" * 80) }

def es_get = { es.prepareGet('raw','_all', it).execute().actionGet().getSourceAsString() }

graph.vertices.each { v ->
    line()
    
    println "${v} - ${v.getProperty('urn')}"
    
    doc = es_get(v.getProperty('urn'))

/*
    f = new OutputStreamWriter(new FileOutputStream("dump-${v}.txt"), 'utf8')
    println "f.encoding: ${f.encoding}"
    f<< doc
    f.close()
*/

    n = objectMapper.readTree doc
    
    /*n.fieldNames().each{ k ->
        line()
        println "${k}: ${n.get(k)}"
    }*/

    [ Direction.IN, Direction.OUT ].each { d ->
        line(); println "\tEdges in direction ${d}"
        
        v.query().direction(d).edges().each { e ->
            println e
        }
    }
    line()
}

null