RMLProcessor with Functions
===========================
This is a branch of the RML Processor (https://github.com/mmlab/RMLProcessor), which is itself based on [DB2Triples](https://github.com/antidot/db2triples/)

Installation and usage
-------
To install

    mvn clean install

To run

    java -jar target/RMLMapper-0.1.jar <mapping> <output>

Running an example

    java -jar target/RMLMapper-0.1.jar functions-example/example.rml.ttl functions-example/output.ttl


License
-------
The RMLProcessor is released under the terms of the [MIT license](http://opensource.org/licenses/mit-license.html).
