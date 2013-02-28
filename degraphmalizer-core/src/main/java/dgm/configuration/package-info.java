/**

 The interfaces in this package define the configuration of the degraphmalizer

 @startuml interface IndexConfig {
 +name() : String
 }

 interface TypeConfig {
 +extract() : SubGraph
 +filter(doc) : boolean
 +name() : String
 }

 interface WalkConfig {
 +direction() : Direction
 +properties() : Map<String, PropertyConfig>
 +name() : String
 }

 interface PropertyConfig {
 +name() : String
 +reduce(Tree<XContent>) : XContent
 }

 IndexConfig <-up- TypeConfig
 TypeConfig <-up- WalkConfig
 WalkConfig <-up- PropertyConfig

 @enduml

 */

package dgm.configuration;
