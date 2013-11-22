/*
	Mechanika
    Copyright (C) 2011 Martin Majer

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 * MechInfoWindow.java
 *
 * Created on 2.6.2011, 19:02:18
 */

package net.martinmajer.mech;

/**
 *
 * @author Martin
 */
public class MechInfoWindow extends javax.swing.JFrame {

    /** Creates new form MechInfoWindow */
    public MechInfoWindow() {
        initComponents();
    }

	public void setText(String text) {
		content.setText(text);
	}

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        textPanel = new javax.swing.JPanel();
        scrollPane = new javax.swing.JScrollPane();
        content = new javax.swing.JTextArea();
        bottomPanel = new javax.swing.JPanel();
        closeButton = new javax.swing.JButton();

        setTitle("Informace o modelu");

        textPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
        textPanel.setPreferredSize(new java.awt.Dimension(600, 400));
        textPanel.setLayout(new java.awt.BorderLayout());

        scrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setPreferredSize(new java.awt.Dimension(400, 300));

        content.setColumns(20);
        content.setEditable(false);
        content.setFont(MechConsts.FNT_TEXT);
        content.setLineWrap(true);
        content.setRows(5);
        content.setWrapStyleWord(true);
        scrollPane.setViewportView(content);

        textPanel.add(scrollPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(textPanel, java.awt.BorderLayout.CENTER);

        bottomPanel.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        closeButton.setText("Zavřít");
        closeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                closeButtonActionPerformed(evt);
            }
        });
        bottomPanel.add(closeButton);

        getContentPane().add(bottomPanel, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

	private void closeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_closeButtonActionPerformed
		setVisible(false);
	}//GEN-LAST:event_closeButtonActionPerformed

	

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottomPanel;
    private javax.swing.JButton closeButton;
    private javax.swing.JTextArea content;
    private javax.swing.JScrollPane scrollPane;
    private javax.swing.JPanel textPanel;
    // End of variables declaration//GEN-END:variables

}
