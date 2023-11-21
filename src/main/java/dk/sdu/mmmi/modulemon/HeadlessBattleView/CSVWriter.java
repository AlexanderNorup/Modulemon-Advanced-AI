package dk.sdu.mmmi.modulemon.HeadlessBattleView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVWriter {
    private char separator = ',';
    private boolean writeExcelHeader = false;
    private String[] columnTitles;

    private List<Object[]> rows;

    public CSVWriter(){
        rows = new ArrayList<>();
    }

    public boolean save(String fileToWriteTo) throws IOException {
        try (var writer = new BufferedWriter(new FileWriter(fileToWriteTo))) {
            if(writeExcelHeader){
                writer.append("sep=").append(separator);
                writer.newLine();
            }

            if(columnTitles != null){
                writer.append(String.join(String.valueOf(separator), columnTitles));
                writer.newLine();
            }

            for(var row : rows){
                boolean first = true; // Used to make sure we don't have trailing separators
                for(var field : row){
                    if(!first){
                        writer.append(separator);
                    }
                    first = false;

                    writer.append(field.toString());
                }
                writer.newLine();
            }

            writer.flush();
        }

        return true;
    }

    public void addRow(Object... row){
        rows.add(row);
    }

    public char getSeparator() {
        return separator;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public String[] getColumnTitles() {
        return columnTitles;
    }

    public void setColumnTitles(String... columnTitles) {
        this.columnTitles = columnTitles;
    }

    public boolean isWriteExcelHeader() {
        return writeExcelHeader;
    }

    public void setWriteExcelHeader(boolean writeExcelHeader) {
        this.writeExcelHeader = writeExcelHeader;
    }
}
