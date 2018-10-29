package cz.iocb.chemweb.server.servlets.endpoint;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import cz.iocb.chemweb.server.db.postgresql.ConnectionPool;
import cz.iocb.chemweb.server.sparql.translator.SparqlDatabaseConfiguration;



@SuppressWarnings("serial")
public class SachemEndpointStatisticsServlet extends HttpServlet
{
    static private final SimpleDateFormat dateFormatGmt;

    private ConnectionPool chebiPool;
    private ConnectionPool chemblPool;
    private ConnectionPool drugbankPool;
    private ConnectionPool pubchemPool;


    static
    {
        dateFormatGmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UTC"));
    }


    @Override
    public void init(ServletConfig config) throws ServletException
    {
        String chebiResourceName = config.getInitParameter("resource-chebi");

        if(chebiResourceName == null || chebiResourceName.isEmpty())
            throw new ServletException("ChEBI resource name is not set");


        String chemblResourceName = config.getInitParameter("resource-chembl");

        if(chemblResourceName == null || chemblResourceName.isEmpty())
            throw new ServletException("ChEMBL resource name is not set");


        String drugbankResourceName = config.getInitParameter("resource-drugbank");

        if(drugbankResourceName == null || drugbankResourceName.isEmpty())
            throw new ServletException("DrugBank resource name is not set");


        String pubchemResourceName = config.getInitParameter("resource-pubchem");

        if(pubchemResourceName == null || pubchemResourceName.isEmpty())
            throw new ServletException("PubChem resource name is not set");


        try
        {
            Context context = (Context) (new InitialContext()).lookup("java:comp/env");

            chebiPool = ((SparqlDatabaseConfiguration) context.lookup(chebiResourceName)).getConnectionPool();
            chemblPool = ((SparqlDatabaseConfiguration) context.lookup(chemblResourceName)).getConnectionPool();
            drugbankPool = ((SparqlDatabaseConfiguration) context.lookup(drugbankResourceName)).getConnectionPool();
            pubchemPool = ((SparqlDatabaseConfiguration) context.lookup(pubchemResourceName)).getConnectionPool();
        }
        catch(NamingException e)
        {
            throw new ServletException(e);
        }
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException
    {
        res.setHeader("access-control-allow-origin", "*");
        res.setHeader("content-type", "application/json");

        try(ServletOutputStream out = res.getOutputStream())
        {
            out.println("{\"datasets\": [");
            writePubChem(out);
            out.println(",");
            writeChembl(out);
            out.println(",");
            writeChebi(out);
            out.println(",");
            writeDrugbank(out);
            out.println("]}");
        }
        catch(SQLException e)
        {
            throw new IOException(e);
        }
    }


    private void writeDrugbank(ServletOutputStream out) throws IOException, SQLException
    {
        try(Connection connection = drugbankPool.getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement.executeQuery("select version, size, checkdate from compound_stats"))
                {
                    result.next();

                    out.println("{");
                    out.println("  \"name\": \"DrugBank\",");
                    out.println("  \"version\": \"" + result.getString(1).replaceAll("-", ".") + "\",");
                    out.println("  \"size\":" + result.getInt(2) + ",");
                    out.println("  \"checked\": \"" + dateFormatGmt.format(result.getTimestamp(3)) + "\"");
                    out.println("}");
                }
            }
        }
    }


    private void writePubChem(ServletOutputStream out) throws IOException, SQLException
    {
        try(Connection connection = pubchemPool.getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement.executeQuery("select version, size, checkdate from compound_stats"))
                {
                    result.next();

                    out.println("{");
                    out.println("  \"name\": \"PubChem\",");
                    out.println("  \"version\": \"" + result.getString(1) + "\",");
                    out.println("  \"size\":" + result.getInt(2) + ",");
                    out.println("  \"checked\": \"" + dateFormatGmt.format(result.getTimestamp(3)) + "\"");
                    out.println("}");
                }
            }
        }
    }


    private void writeChembl(ServletOutputStream out) throws IOException, SQLException
    {
        try(Connection connection = chemblPool.getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement.executeQuery(
                        "select src.name, stat.size, stat.checkdate from compound_stats stat, compound_sources src"))
                {
                    result.next();

                    out.println("{");
                    out.println("  \"name\": \"ChEMBL\",");
                    out.println("  \"version\": \""
                            + result.getString(1).replaceAll("^chembl_", "").replaceAll("\\.sdf\\.gz", "") + "\",");
                    out.println("  \"size\":" + result.getInt(2) + ",");
                    out.println("  \"checked\": \"" + dateFormatGmt.format(result.getTimestamp(3)) + "\"");
                    out.println("}");
                }
            }
        }
    }


    private void writeChebi(ServletOutputStream out) throws IOException, SQLException
    {
        SimpleDateFormat versionFormatGmt = new SimpleDateFormat("yyyy-MM-dd");

        try(Connection connection = chebiPool.getConnection())
        {
            try(Statement statement = connection.createStatement())
            {
                try(ResultSet result = statement.executeQuery(
                        "select src.timestamp, stat.size, stat.checkdate from compound_stats stat, compound_sources src"))
                {
                    result.next();

                    out.println("{");
                    out.println("  \"name\": \"ChEBI\",");
                    out.println("  \"version\": \"" + versionFormatGmt.format(result.getTimestamp(1)) + "\",");
                    out.println("  \"size\":" + result.getInt(2) + ",");
                    out.println("  \"checked\": \"" + dateFormatGmt.format(result.getTimestamp(3)) + "\"");
                    out.println("}");
                }
            }
        }
    }
}
