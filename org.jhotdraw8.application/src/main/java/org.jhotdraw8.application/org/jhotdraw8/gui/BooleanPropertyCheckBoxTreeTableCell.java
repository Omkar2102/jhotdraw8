/*
 * @(#)BooleanPropertyCheckBoxTreeTableCell.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.gui;

import javafx.beans.Observable;
import javafx.beans.property.Property;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.util.Callback;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;

/**
 * BooleanPropertyCheckBoxTreeTableCell.
 *
 * @param <S> the row type
 * @author Werner Randelshofer
 */
public class BooleanPropertyCheckBoxTreeTableCell<S> extends TreeTableCell<S, Boolean> {

    private final CheckBox checkBox = new CheckBox();

    public BooleanPropertyCheckBoxTreeTableCell(@Nullable String styleClass) {
        checkBox.selectedProperty().addListener(this::commitSelectionChange);
        if (styleClass != null) {
            checkBox.getStyleClass().add(styleClass);
        }
    }

    @Override
    public void updateItem(@Nullable Boolean item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(" ");// must be non-empty to make indentation work
            setGraphic(checkBox);
            checkBox.setSelected(item);
        }
    }

    public static @NonNull <S> Callback<TreeTableColumn<S, Boolean>, TreeTableCell<S, Boolean>> forTreeTableColumn() {
        return forTreeTableColumn(null);
    }

    public static @NonNull <S> Callback<TreeTableColumn<S, Boolean>, TreeTableCell<S, Boolean>> forTreeTableColumn(@Nullable String styleClass) {
        return list -> new BooleanPropertyCheckBoxTreeTableCell<>(styleClass);
    }

    private void commitSelectionChange(Observable o) {
        Property<Boolean> p = (Property<Boolean>) getTableColumn().getCellObservableValue(getIndex());
        // Note: We must not call setValue if the value has not changed, because
        // setValue will fire an invalidation event that we do not want.
        if (p.getValue() != checkBox.isSelected()) {
            p.setValue(checkBox.isSelected());
        }
    }
}
