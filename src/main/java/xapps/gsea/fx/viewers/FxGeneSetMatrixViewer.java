/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.objects.GeneSet;
import edu.mit.broad.genome.objects.GeneSetMatrix;
import edu.mit.broad.genome.parsers.AuxUtils;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Gene set matrix viewer: full membership matrix (one column per gene set) plus summary
 * info.
 */
public class FxGeneSetMatrixViewer implements ViewPage {

    private final GeneSetMatrix matrix;
    private final BorderPane root = new BorderPane();

    public FxGeneSetMatrixViewer(GeneSetMatrix matrix) {
        this.matrix = matrix;

        Label title = new Label(matrix.getName() + "  (" + matrix.getNumGeneSets() + " gene sets)");
        title.setPadding(new Insets(8, 12, 4, 12));
        title.getStyleClass().add("gsea-section-header");

        TabPane tabs = new TabPane();
        tabs.setSide(javafx.geometry.Side.BOTTOM);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab dataTab = new Tab("Data", buildDataPane());
        dataTab.setClosable(false);
        Tab infoTab = new Tab("Info", buildInfoPane());
        infoTab.setClosable(false);
        tabs.getTabs().addAll(dataTab, infoTab);

        VBox box = new VBox(4, title, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.setCenter(box);
    }

    private TableView<Integer> buildDataPane() {
        int numSets = matrix.getNumGeneSets();
        int maxSize = matrix.getMaxGeneSetSize();

        ObservableList<Integer> rows = FXCollections.observableArrayList();
        for (int r = 0; r < maxSize; r++) {
            rows.add(r);
        }

        TableView<Integer> table = new TableView<>(rows);
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        TableColumn<Integer, String> numCol = new TableColumn<>(" ");
        numCol.setCellValueFactory(data -> new SimpleStringProperty(Integer.toString(data.getValue() + 1)));
        numCol.setPrefWidth(50);
        table.getColumns().add(numCol);
        for (int c = 0; c < numSets; c++) {
            final int col = c;
            TableColumn<Integer, String> column = new TableColumn<>(
                    AuxUtils.getAuxNameOnlyNoHash(matrix.getGeneSet(c).getName()));
            column.setCellValueFactory(data -> {
                int r = data.getValue();
                GeneSet gs = matrix.getGeneSet(col);
                String val = gs.getNumMembers() > r ? gs.getMember(r) : "";
                return new SimpleStringProperty(val);
            });
            column.setPrefWidth(150);
            table.getColumns().add(column);
        }
        return table;
    }

    private TextArea buildInfoPane() {
        StringBuilder buf = new StringBuilder("Name: ").append(matrix.getName()).append('\n');
        buf.append("Number of sets: ").append(matrix.getNumGeneSets()).append('\n');
        buf.append("Total number of unique features: ").append(matrix.getAllMemberNamesOnlyOnceS().size()).append('\n');

        String comment = matrix.getComment();
        if (comment != null && comment.length() > 0) {
            buf.append("\nComments\n");
            buf.append(comment).append('\n');
        }

        buf.append("\nGene Set Names\n");
        for (int i = 0; i < matrix.getNumGeneSets(); i++) {
            buf.append(matrix.getGeneSet(i).getName()).append('\t')
                    .append(matrix.getGeneSet(i).getNumMembers()).append('\n');
        }

        TextArea textArea = new TextArea(buf.toString());
        textArea.setEditable(false);
        return textArea;
    }

    @Override
    public String getTitle() {
        return matrix.getName();
    }

    @Override
    public String getIconResourceId() {
        return "Gmx.png";
    }

    @Override
    public javafx.scene.Node getContent() {
        return root;
    }
}
