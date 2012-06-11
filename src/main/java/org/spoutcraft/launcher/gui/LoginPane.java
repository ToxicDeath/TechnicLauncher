package org.spoutcraft.launcher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import net.miginfocom.swing.MigLayout;
import java.awt.Insets;

/*
 * TODO: Make sure focus order is correct
 * 
 */

/**
 * The Login fields and buttons
 * 
 * @author sir maniac
 */
public class LoginPane extends JPanel {
	private static final long serialVersionUID = 1L;

	private JPasswordField passwordField;
	private JComboBox usernameField;
	private JButton loginButton;
	private JButton optionsButton;
	private JButton modSelectButton;
	private JCheckBox rememberCheckbox;

	private LoginPane.Listener listener = null;
	
	private UserPasswordComboModel userPasswordModel;
	
	private static final Document PLACEHOLDER_PASSWORD = new PlainDocument(); 
	
	public LoginPane(Font font) {
	setPreferredSize(new Dimension(173, 193));
	setMinimumSize(new Dimension(145, 164));
		Color background = getBackground();
		// set alpha 
		setBackground(new Color(background.getRed(),  
		                        background.getGreen(),
		                        background.getBlue(),
		                        128
									));		
		//setBorder(new EmptyBorder(new Insets(10,10,10,10)));

		setLayout(new MigLayout("fill, insets 10"));
		
		JLabel lblUserName = new JLabel("User Name");
		lblUserName.setFont(font);
		lblUserName.setVerticalAlignment(SwingConstants.BOTTOM);
		lblUserName.setHorizontalAlignment(SwingConstants.LEFT);
		//lblUserName.setMaximumSize(new Dimension(Short.MAX_VALUE, height));
		add(lblUserName, "spanx ,alignx left,aligny bottom");

		passwordField = new JPasswordField();
		rememberCheckbox = new JCheckBox("Remember");
		
		userPasswordModel = new UserPasswordComboModel(passwordField, 
		                                               rememberCheckbox); 

		/*
		 * TODO: Add a history drop-box when typing
		 */
		usernameField = new JComboBox(userPasswordModel);
		usernameField.setEditable(true);
		usernameField.setFont(font);
		usernameField.setBorder(null);
		
		usernameField.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				/*
				 * FIXME: can possibly be bound to to the "comboBoxEdited" actionCommand
				 * 
				 * I didn't do this because it's not mentioned in the API docs, so 
				 * might not be guaranteed to work in every VM.
				 */
				
				passwordField.requestFocusInWindow();
			}
		});
		
		add(usernameField, "spanx ,growx, aligny top");
		
		JLabel lblPassword = new JLabel("Password");
		lblPassword.setFont(font);
		lblPassword.setVerticalAlignment(SwingConstants.BOTTOM);
		lblPassword.setHorizontalAlignment(SwingConstants.LEFT);
		add(lblPassword, "spanx ,alignx left,aligny bottom");
		
		passwordField.setFont(font); // keep the the same size as usernameField 
		passwordField.setColumns(10);
		passwordField.addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent e) {
			}
			
			@Override
			public void focusGained(FocusEvent e) {
				if (passwordField.getDocument() == PLACEHOLDER_PASSWORD) {
					userPasswordModel.setSelectedItem(usernameField.getEditor().getItem());
				}
			}
		});
		passwordField.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) { }
			@Override
			public void keyReleased(KeyEvent e) { }
			@Override
			public void keyPressed(KeyEvent e) {
				if (listener != null && loginButton.isEnabled() && 
					e.getKeyCode() == KeyEvent.VK_ENTER) {
					
					listener.doLogin((String)userPasswordModel.getSelectedItem(), 
							userPasswordModel.getSelectedPassword());
				}
			}
		});
		
		
		
		add(passwordField, "spanx ,growx,aligny top");
		
		rememberCheckbox.setFont(font);
		rememberCheckbox.setVerticalTextPosition(SwingConstants.BOTTOM);
		rememberCheckbox.setHorizontalTextPosition(SwingConstants.LEADING);
		rememberCheckbox.setMargin(new Insets(0, 0, 0, 0));
		add(rememberCheckbox, "spanx ,alignx left,aligny center");
		
		loginButton = new JButton("Login");
		loginButton.setFont(font);
		loginButton.setMaximumSize(new Dimension(Short.MAX_VALUE, 19));
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener != null) {
					listener.doLogin((String)userPasswordModel.getSelectedItem(), 
							userPasswordModel.getSelectedPassword());
				}
			}
		});
		add(loginButton, "growx 50");
		
		optionsButton = new JButton("Options");
		optionsButton.setFont(font);
		optionsButton.setMaximumSize(new Dimension(Short.MAX_VALUE, 19));
		optionsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener != null) {
					listener.doOptions();	
				}
			}
		});
		add(optionsButton, "growx 50,wrap");
		
		modSelectButton = new JButton("Mod Select");
		modSelectButton.setFont(font);
		modSelectButton.setMaximumSize(new Dimension(Short.MAX_VALUE, 19));
		modSelectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (listener != null) {
					listener.doModSelect();
				}
			}
		});
		add(modSelectButton, "spanx ,growx");
		
		
	}
	
	public void setLoginEnabled(boolean enabled) {
		loginButton.setEnabled(enabled);
	}
	
	public boolean isLoginEnabled(boolean enabled) {
		return loginButton.isEnabled();
	}

	public void setOptionsEnabled(boolean enabled) {
		optionsButton.setEnabled(enabled);
	}
	
	public boolean isOptionsEnabled(boolean enabled) {
		return optionsButton.isEnabled();
	}
	
	public void setModSelectEnabled(boolean enabled) {
		modSelectButton.setEnabled(enabled);
	}
	
	public boolean isModSelectEnabled(boolean enabled) {
		return modSelectButton.isEnabled();
	}
	
//	public void setAllEnabled(boolean enabled) {
//		loginButton.setEnabled(enabled);
//		optionsButton.setEnabled(enabled);
//		modSelectButton.setEnabled(enabled);
//	}
//	
//	public boolean isAllEnabled() {
//		return loginButton.isEnabled() && optionsButton.isEnabled();
//	}
	
	public void addUser(String user, String pass, boolean remembered) {
		userPasswordModel.addElement(user, pass, remembered);
	}
	
	public String getCurrentUser() {
		return (String)userPasswordModel.getSelectedItem();
	}
	
	public String getCurrentPassword() {
		return userPasswordModel.getSelectedPassword();
	}
	
	public boolean isCurrentRemembered() {
		return userPasswordModel.isSelectedRemembered();
	}
	
	public UserPasswordComboModel getUserPasswordModel() {
		return userPasswordModel;
	}
	
	public void setListener(LoginPane.Listener l) {
		listener = l;
	}

	public static interface Listener {
		public void doLogin(String user, String pass);
		public void doOptions();
		public void doModSelect(); 
	}
	
	/**
	 * A ComboBoxModel that syncs the usernameField to the passwordField and 
	 * 	remember checkbox.
	 * 
	 * @author sir maniac
	 */
	public static class UserPasswordComboModel implements MutableComboBoxModel {
		private JPasswordField passwordField;
		private JCheckBox rememberChkBox;
		
		private List<ListDataListener> dataListeners = new ArrayList<ListDataListener>();
		
		private List<String> users = new ArrayList<String>();
		private List<Document> passes = new ArrayList<Document>();
		private List<Boolean> remembers = new ArrayList<Boolean>();
		private int curIndex;
		
		public UserPasswordComboModel(JPasswordField passwordField, 
		                              JCheckBox rememberChkBox) {
			curIndex = -1;
			
			this.passwordField = passwordField;
			
			
			this.rememberChkBox = rememberChkBox;
			rememberChkBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (remembers.size() > 0 && curIndex >= 0) {
						int change =  e.getStateChange();
						if (change == ItemEvent.SELECTED) {
							remembers.set(curIndex, true);
						} else if (change == ItemEvent.DESELECTED) {
							remembers.set(curIndex, false);
						}
					}
				}
			});
		}
		
		/**
		 * Add or replace user with an empty password that isn't remembered
		 */
		@Override
		public final void addElement(Object user) {
			if (!(user instanceof String)) {
				throw new IllegalArgumentException("argument must be a string");
			}			// TODO Auto-generated method stub
			
			addElement((String)user, null, false);
		}
		
		/**
		 * Add or replace user with a password, if password is not blank, 
		 *  it is automatically remembered
		 *  
		 * @param user the user name displayed in the combobox
		 * @param pass password or null, if password is null and user exists, 
		 * 				the current password is preserved
		 */
		public void addElement(String user, String pass) {
			addElement(user, pass, (pass != null && pass.isEmpty()));
		}

		/**
		 * Add or replace user with it's password, and whether or not it's remembered
		 * 
		 * @param user the user name displayed in the combobox
		 * @param pass password or null, if password is null and user exists, 
		 * 				the current password is preserved
		 * @param whether or not to remember the password
		 * 
		 */
		public void addElement(String user, String pass, boolean remembered) {
			
			PlainDocument passDoc = new PlainDocument();
			try {
				passDoc.insertString(0, pass, null);
			} catch (BadLocationException e) {
				throw new RuntimeException("unexpected exception", e);
			}
			
			int i = users.indexOf(user);
			if (i >= 0) {
				if (pass != null)	{
					replaceInDocument(passes.get(i), pass);
					remembers.set(i, remembered);
				}
			} else {
				users.add(user);
				if (pass == null) {
					pass = "";
				}
				passes.add(newDocument(pass));
				remembers.add(remembered);

				for (ListDataListener l : dataListeners) {
					l.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, users.size()-1, users.size()));
				}
			}
			
			// first addition becomes current entry
			if (users.size() == 1) {
				curIndex=0;
				passwordField.setDocument(passes.get(0));
				rememberChkBox.setSelected(remembers.get(0));
			}
			
		}
		
		@Override
		public void setSelectedItem(Object user) {
			int index = users.indexOf(user);
			
			if(index < 0) {
				curIndex = users.size();
				addElement(user);
			} else {
				curIndex = index;
				passwordField.setDocument(passes.get(index));
				rememberChkBox.setSelected(remembers.get(index));
			}
		}
		
		public int indexOf(String user) {
			return users.indexOf(user);
		}

		public void setSelectedPassword(String pass) {
			if (curIndex < 0) {
				return;
			}
			
			replaceInDocument(passes.get(curIndex), pass);
		}
		
		public void setSelectedRemembered(boolean remembered) {
			if (curIndex < 0) {
				return;
			}
			
			rememberChkBox.setSelected(remembered);
			remembers.set(curIndex, remembered);
		}

		public boolean isSelectedRemembered() {
			if (curIndex < 0) {
				return false;
			}
			
			return remembers.get(curIndex);
		}

		public boolean isRemembered(String user) {
			int index = users.indexOf(user);
			if (index < 0) {
				return false;
			}
			
			return remembers.get(index);
		}
		
		public String getPassword(String user) {
			int index = users.indexOf(user);
			if (index < 0) {
				return null;
			}
			
			Document doc = passes.get(index);
			try {
				return doc.getText(0, doc.getLength());
			} catch (BadLocationException e) {
				throw new RuntimeException("Unexpected Exception", e);
			}
		}
		
		public String getSelectedPassword() {
			if (curIndex < 1) {
				return null;
			}
			
			Document doc = passes.get(curIndex);
			try {
				return doc.getText(0, doc.getLength());
			} catch (BadLocationException e) {
				throw new RuntimeException("Unexpected Exception", e);
			}
		}

		@Override
		public Object getSelectedItem() {
			if (curIndex < 0) {
				return null;
			}
			
			return users.get(curIndex);
		}

		@Override
		public Object getElementAt(int index) throws IndexOutOfBoundsException {
			return users.get(index);
		}

		public String getPasswordAt(int index)  throws IndexOutOfBoundsException {
			Document doc = passes.get(index);
			
			try {
				return doc.getText(0, doc.getLength());
			} catch (BadLocationException e) {
				throw new RuntimeException("Unexpected exception", e);
			}
		}

		public boolean getRememberedAt(int index) {
			return remembers.get(index);
		}
		
		@Override
		public int getSize() {
			return users.size();
		}

		@Override
		public void removeElement(Object obj) {
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		public void insertElementAt(Object obj, int index) {
			throw new UnsupportedOperationException("not implemented");
		}

		@Override
		public void removeElementAt(int index) {
			throw new UnsupportedOperationException("not implemented");
		}
		
		@Override
		public void addListDataListener(ListDataListener l) {
			dataListeners.add(l);
		}

		@Override
		public void removeListDataListener(ListDataListener l) {
			dataListeners.remove(l);
		}

		private void replaceInDocument(Document doc, String str) {
			try {
				doc.remove(0, doc.getLength());
				doc.insertString(0, str, null);
			} catch (BadLocationException e) {
				throw new RuntimeException("unexpected exception", e);
			}
		}

		private Document newDocument(String str) {
			Document doc = new PlainDocument();
			try {
				doc.insertString(0, str, null);
				return doc;
			} catch (BadLocationException e) {
				throw new RuntimeException("unexpected exception", e);
			}
		}
	}
}
	
