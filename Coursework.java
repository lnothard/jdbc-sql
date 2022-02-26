import java.sql.*;
import java.util.ArrayList;

public class Coursework {

    static Table[] tables;

    public static void main(String[] args) {
        Connection c = null;
        Statement stmt = null;
        DatabaseMetaData metadata = null;
        ResultSet[] columns;

        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:University.db");

            stmt = c.createStatement();
            ResultSet count = stmt.executeQuery("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table';");

            int numTables = count.getInt(1);
            count.close();

            metadata = c.getMetaData();
            ResultSet rs = metadata.getTables(null, null, null, new String[] {"TABLE"});

            tables = new Table[numTables];

            int i = 0;
            while (rs.next()){
                ResultSet col = stmt.executeQuery("SELECT * FROM " + rs.getString("TABLE_NAME") + ";");
                tables[i] = new Table(rs.getString("TABLE_NAME"), col.getMetaData().getColumnCount());
                i++;
                col.close();
            }

            rs.close();

            for (int x = 0; x < tables.length; x++) {
                ResultSet PK = metadata.getPrimaryKeys(null, null, tables[x].name);
                while (PK.next()) {
                    tables[x].primaryKeys.add(PK.getString("COLUMN_NAME"));
                }
                PK.close();

                ResultSet FK = metadata.getImportedKeys(null, null, tables[x].name);
                while (FK.next()) {
                    String s = "FOREIGN KEY (" + FK.getString("FKCOLUMN_NAME") + ") REFERENCES " + FK.getString("PKTABLE_NAME") + "(" + FK.getString("PKCOLUMN_NAME") + ")";
                    tables[x].foreignKeys.add(s);
                    tables[x].references.add(FK.getString("PKTABLE_NAME"));
                }
                FK.close();

                int n = 0;
                ResultSet index = metadata.getIndexInfo(null, null, tables[x].name, true, false);
                while (index.next()) {
                    String s = "CREATE INDEX " + tables[x].name + n + " ON " + index.getString("TABLE_NAME") + " (" + index.getString("COLUMN_NAME") + " " + "ASC);"; 
                    tables[x].indexes.add(s);
                    n++;
                }
                index.close();
            }


            int j = 0;
            columns = new ResultSet[tables.length];

            while (j < tables.length) {
                columns[j] = metadata.getColumns(null, null, tables[j].name, null);
                int k = 0;
                while (columns[j].next()) {
                    tables[j].columns[k] = new Column(columns[j].getString("COLUMN_NAME"), columns[j].getString("TYPE_NAME"), columns[j].getString("DECIMAL_DIGITS"));
                    k++;
                }
                j++;
            }
            
            for (int l = 0; l < tables.length; l++) {
                ResultSet row = stmt.executeQuery("SELECT * FROM " + tables[l].name + ";");
                
                while (row.next()) {
                    String r = "";
                    for (int m = 0; m < tables[l].columns.length; m++) {
                        if (tables[l].columns[m].columnType.equals("INT")) 
                            r += String.valueOf(row.getString(tables[l].columns[m].columnName)) + ",";
                        else
                            r += "'" + row.getString(tables[l].columns[m].columnName) + "'" + ","; 
                    }
                    tables[l].rows.add(r);
                }
            }

            String string = "";

            for (int n = 0; n < tables.length; n++) {
                string += "CREATE TABLE " + tables[n].name + " (\n\t";

                for (int o = 0; o < tables[n].columns.length; o++) 
                    string += tables[n].columns[o].columnName + " " + tables[n].columns[o].columnType + ",\n\t";
                
                string += "PRIMARY KEY (";
                for (int q = 0; q < tables[n].primaryKeys.size(); q++) {
                    string += tables[n].primaryKeys.get(q);
                    if (q == tables[n].primaryKeys.size() - 1 && tables[n].foreignKeys.size() == 0)
                        string += ")\n\t";
                    else if (q == tables[n].primaryKeys.size() - 1 && tables[n].foreignKeys.size() != 0)
                        string += "),\n\t";
                    else
                        string += ", ";
                }

                for (int z = 0; z < tables[n].foreignKeys.size(); z++) {
                    string += tables[n].foreignKeys.get(z);
                    if (z == tables[n].foreignKeys.size() - 1)
                        string += "\n\t";
                    else
                        string += ",\n\t";
                }

                string += ");\n\n";
            }

            for (int p = 0; p < tables.length; p++) {
                for (String s: tables[p].rows) {
                    string += "INSERT INTO " + tables[p].name + " VALUES(" + s.substring(0, s.length() - 1) + ");" + '\n';
                }
            }

            for (int y = 0; y < tables.length; y++) {
                for (String index: tables[y].indexes) {
                    string += index + '\n';
                }
            }

            System.out.print(string);

            stmt.close();
            c.close();

        } catch (Exception e) {
            System.err.println(e.getClass().getName() + ": " + e.getMessage());
            System.exit(0);
        }
    }

    static class Table {

        String name;
        Column[] columns;
        ArrayList<String> rows = new ArrayList<>();
        ArrayList<String> primaryKeys = new ArrayList<>();
        ArrayList<String> foreignKeys = new ArrayList<>();
        ArrayList<String> references = new ArrayList<>();
        ArrayList<String> indexes = new ArrayList<>();

        Table(String name, int numColumns) {
            this.name = name;
            columns = new Column[numColumns];
        }
    }

    static class Column {

        String columnName;
        String columnType;
        String numDigits;

        Column(String columnName, String columnType, String numDigits) {
            this.columnName = columnName;
            this.columnType = columnType;
            this.numDigits = numDigits;
        }
    }
}