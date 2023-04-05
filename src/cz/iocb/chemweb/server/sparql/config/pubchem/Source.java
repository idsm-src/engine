package cz.iocb.chemweb.server.sparql.config.pubchem;

import static cz.iocb.chemweb.server.sparql.config.pubchem.PubChemConfiguration.schema;
import static cz.iocb.chemweb.server.sparql.mapping.classes.BuiltinClasses.xsdString;
import cz.iocb.chemweb.server.sparql.config.SparqlDatabaseConfiguration;
import cz.iocb.chemweb.server.sparql.database.Table;
import cz.iocb.chemweb.server.sparql.database.TableColumn;
import cz.iocb.chemweb.server.sparql.mapping.ConstantIriMapping;
import cz.iocb.chemweb.server.sparql.mapping.NodeMapping;
import cz.iocb.chemweb.server.sparql.mapping.classes.ListUserIriClass;
import cz.iocb.chemweb.server.sparql.mapping.classes.MapUserIriClass;



public class Source
{
    public static void addResourceClasses(SparqlDatabaseConfiguration config)
    {
        config.addIriClass(new MapUserIriClass("pubchem:source", "smallint", new Table("pubchem", "source_bases"),
                new TableColumn("id"), new TableColumn("iri"), "http://rdf.ncbi.nlm.nih.gov/pubchem/source/"));
        config.addIriClass(new ListUserIriClass("pubchem:source-license", new Table("pubchem", "source_bases"),
                new TableColumn("license")));
        config.addIriClass(new ListUserIriClass("pubchem:source-homepage", new Table("pubchem", "source_bases"),
                new TableColumn("homepage")));
    }


    public static void addQuadMappings(SparqlDatabaseConfiguration config)
    {
        ConstantIriMapping graph = config.createIriMapping("pubchem:source");

        {
            Table table = new Table(schema, "source_bases");
            NodeMapping subject = config.createIriMapping("pubchem:source", "id");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("rdf:type"),
                    config.createIriMapping("dcterms:Dataset"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:license"),
                    config.createIriMapping("pubchem:source-license", "license"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("foaf:homepage"),
                    config.createIriMapping("pubchem:source-homepage", "homepage"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:title"),
                    config.createLiteralMapping(xsdString, "title"));
            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:rights"),
                    config.createLiteralMapping(xsdString, "rights"));

            // extension
            config.addQuadMapping(table, graph, subject, config.createIriMapping("template:itemTemplate"),
                    config.createLiteralMapping("pubchem/Source.vm"));
        }

        {
            Table table = new Table(schema, "source_subjects");
            NodeMapping subject = config.createIriMapping("pubchem:source", "source");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:subject"),
                    config.createIriMapping("pubchem:concept", "subject"));
        }

        {
            Table table = new Table(schema, "source_alternatives");
            NodeMapping subject = config.createIriMapping("pubchem:source", "source");

            config.addQuadMapping(table, graph, subject, config.createIriMapping("dcterms:alternative"),
                    config.createLiteralMapping(xsdString, "alternative"));
        }
    }
}
