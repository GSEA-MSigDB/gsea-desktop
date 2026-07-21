/*
 * Copyright (c) 2003-2026 Broad Institute, Inc., Massachusetts Institute of Technology, and Regents of the University of California. All rights reserved.
 */
package xapps.gsea.fx.viewers;

import org.gsea_msigdb.gsea.ui.api.ViewPage;

import edu.mit.broad.genome.Printf;
import edu.mit.broad.genome.objects.Template;
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

/** Phenotype / template viewer: class-level structure, cls-format text, and summary info. */
public class FxPhenotypeViewer implements ViewPage {

    private final Template template;
    private final BorderPane root = new BorderPane();

    public FxPhenotypeViewer(Template template) {
        this.template = template;

        Label title = new Label(template.getName() + "  (" + template.getNumItems() + " samples, "
                + template.getNumClasses() + " classes)");
        title.setPadding(new Insets(8, 12, 4, 12));
        title.getStyleClass().add("gsea-section-header");

        TabPane tabs = new TabPane();
        tabs.setSide(javafx.geometry.Side.BOTTOM);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab structureTab = new Tab("Phenotype Structure", buildStructurePane());
        structureTab.setClosable(false);
        Tab textTab = new Tab("Phenotype Text", buildTextPane());
        textTab.setClosable(false);
        Tab infoTab = new Tab("Phenotype Info", buildInfoPane());
        infoTab.setClosable(false);
        tabs.getTabs().addAll(structureTab, textTab, infoTab);

        VBox box = new VBox(4, title, tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        root.setCenter(box);
    }

    private TableView<ClassRow> buildStructurePane() {
        TableView<ClassRow> table = new TableView<>();
        TableColumn<ClassRow, String> nameCol = new TableColumn<>("Class Name");
        nameCol.setCellValueFactory(c -> c.getValue().name);
        nameCol.setPrefWidth(200);
        TableColumn<ClassRow, String> idCol = new TableColumn<>("Class Id");
        idCol.setCellValueFactory(c -> c.getValue().id);
        idCol.setPrefWidth(140);
        TableColumn<ClassRow, String> countCol = new TableColumn<>("Class Count");
        countCol.setCellValueFactory(c -> c.getValue().count);
        countCol.setPrefWidth(120);
        table.getColumns().add(nameCol);
        table.getColumns().add(idCol);
        table.getColumns().add(countCol);

        ObservableList<ClassRow> rows = FXCollections.observableArrayList();
        for (int i = 0; i < template.getNumClasses(); i++) {
            Template.Class cl = template.getClass(i);
            // classId is really the template id, taken from any item of the class
            String id = cl.getSize() > 0 ? cl.getItem(0).getId() : "";
            rows.add(new ClassRow(template.getClassName(i), id, Integer.toString(cl.getSize())));
        }
        table.setItems(rows);
        return table;
    }

    private TextArea buildTextPane() {
        TextArea textArea = new TextArea(template.getAsString(false));
        textArea.setEditable(false);
        return textArea;
    }

    private TextArea buildInfoPane() {
        TextArea textArea = new TextArea(Printf.outs(template).toString());
        textArea.setEditable(false);
        return textArea;
    }

    private static final class ClassRow {
        final SimpleStringProperty name;
        final SimpleStringProperty id;
        final SimpleStringProperty count;

        ClassRow(String name, String id, String count) {
            this.name = new SimpleStringProperty(name);
            this.id = new SimpleStringProperty(id);
            this.count = new SimpleStringProperty(count);
        }
    }

    @Override
    public String getTitle() {
        return template.getName();
    }

    @Override
    public String getIconResourceId() {
        return "Cls.gif";
    }

    @Override
    public Object getContent() {
        return root;
    }
}
