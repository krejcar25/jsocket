package net.jsocket.test;

import net.jsocket.*;
import net.jsocket.client.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

public class ClientWindow extends JFrame {

    private JPanel root;
    private DefaultListModel<ChatItem> chatListModel;
    private JList<ChatItem> chatList;
    private JButton sendButton;
    private JScrollPane chatPane;
    private JTextArea chatArea;
    private JButton payloadTestButton;
    private JSpinner testPayloadByteSizeSpinner;
    private JButton changeNicknameButton;
    Client client;

    public ClientWindow(String host, int port) {
        super("JSocket Demo App Client");
        setContentPane(root);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        chatListModel = new DefaultListModel<>();
        chatList.setModel(chatListModel);
        client = new Client(host, port, clientID -> chatList.setCellRenderer(new ChatItemRenderer(clientID.toString(), clientID)), (ID, disconnectReason) -> dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING)));
        client.addHandle("chatMessage", this::client_chatMessageHandle);
        client.addHandle("payloadSentMessage", this::client_payloadSentMessageHandle);
        sendButton.addActionListener(this::sendButton_click);
        payloadTestButton.addActionListener(this::payloadTestButton_click);
        setMinimumSize(new Dimension(400, 400));
        InputMap input = chatArea.getInputMap();
        KeyStroke enter = KeyStroke.getKeyStroke("ENTER");
        KeyStroke shiftEnter = KeyStroke.getKeyStroke("shift ENTER");
        input.put(shiftEnter, "insert-break");
        input.put(enter, "text-submit");

        ActionMap actions = chatArea.getActionMap();
        actions.put("text-submit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });
        testPayloadByteSizeSpinner.setValue(50000000);
        changeNicknameButton.addActionListener(this::changeNicknameButton_click);
    }

    private void changeNicknameButton_click(ActionEvent actionEvent) {
        ChangeNicknameDialog dialog = new ChangeNicknameDialog(client.getClientID().toString());
        dialog.pack();
        dialog.setVisible(true);
        if (dialog.isConfirmed()) nicknameChanged(dialog.getNewName());
    }

    private void nicknameChanged(String newName) {

    }

    private void payloadTestButton_click(ActionEvent actionEvent) {
        sendPayload();
    }

    private void sendButton_click(ActionEvent e) {
        send();
    }

    public void chatArea_keyTyped(KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            send();
        }
    }

    private void send() {
        if (!chatArea.getText().equals("")) {
            System.out.println(chatArea.getText());
            DataCarrier carrier = new DataCarrier<>("chatMessage", Direction.ToServer, ConversationOrigin.ClientBroadcast, new SocketPeerID(client.getClientID()), SocketPeerID.Broadcast, new TextMessage(chatArea.getText()));
            client.send(carrier);
        }
        chatArea.setText("");
    }

    private void sendPayload() {
        System.out.println("Begin send test payload");
        client.send(new DataCarrier<>("payloadTest", Direction.ToServer, ConversationOrigin.ClientToServer, new SocketPeerID(client.getClientID()), SocketPeerID.Server, new PayloadTest((Integer) testPayloadByteSizeSpinner.getValue())));
        System.out.println("Payload sent");
    }

    private void client_chatMessageHandle(DataCarrier<TextMessage> dataCarrier) {
        TextMessage message = dataCarrier.getData();
        chatListModel.addElement(new TextChatItem(dataCarrier.getSenderID().getPeerID().toString(), message.getMessage(), message.getTimestamp()));

        chatPane.getVerticalScrollBar().setValue(chatPane.getHorizontalScrollBar().getMaximum());
    }

    private void client_payloadSentMessageHandle(DataCarrier<PayloadSentChatItem> dataCarrier) {
        chatListModel.addElement(dataCarrier.getData());
    }
}
