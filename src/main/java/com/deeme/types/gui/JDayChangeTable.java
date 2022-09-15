package com.deeme.types.gui;

import com.deeme.types.config.Hour;
import com.deeme.types.config.WeeklyConfig;
import com.github.manolo8.darkbot.gui.tree.OptionEditor;
import com.github.manolo8.darkbot.gui.tree.components.InfoTable;
import com.github.manolo8.darkbot.gui.utils.GenericTableModel;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Arrays;

public class JDayChangeTable extends InfoTable<GenericTableModel, Hour> implements OptionEditor {
    private static final String[] VALUES = { "Stop", "P1", "P2", "P3", "P4" };

    public JDayChangeTable(WeeklyConfig weeklyConfig) {
        super(Hour.class, weeklyConfig.Hours_Changes);
        DefaultCellEditor editor = new DefaultCellEditor(new JComboBox<>(VALUES));
        RenderColors render = new RenderColors();
        super.setDefaultEditor(String.class, editor);
        super.setDefaultRenderer(String.class, render);

        super.getRowSorter().setSortKeys(Arrays.asList(new RowSorter.SortKey(0, SortOrder.ASCENDING)));
        super.updateUI();
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        super.setValueAt(value, row, column);
    }

    public static class RenderColors extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int col) {
            if (value.toString().contains("Stop")) {
                setBackground(Color.lightGray);
            } else if (value.toString().contains("P1")) {
                setBackground(new Color(169, 104, 54));
            } else if (value.toString().contains("P2")) {
                setBackground(new Color(73, 104, 54));
            } else if (value.toString().contains("P3")) {
                setBackground(new Color(133, 118, 144));
            } else if (value.toString().contains("P4")) {
                setBackground(new Color(97, 141, 176));
            } else {
                setBackground(Color.DARK_GRAY);
            }
            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
        }

    }

}
