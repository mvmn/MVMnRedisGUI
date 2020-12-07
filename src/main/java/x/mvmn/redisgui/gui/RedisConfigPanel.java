package x.mvmn.redisgui.gui;

import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import x.mvmn.redisgui.model.RedisConfigModel;

public class RedisConfigPanel extends JPanel {
	private static final long serialVersionUID = -4990237494301838452L;

	private volatile boolean dirty;
	private final DocumentListener dirtyListener = new DocumentListener() {

		@Override
		public void removeUpdate(DocumentEvent e) {
			setDirty();
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			setDirty();
		}

		@Override
		public void changedUpdate(DocumentEvent e) {
			setDirty();
		}
	};
	private final CopyOnWriteArrayList<Runnable> dirtyListeners = new CopyOnWriteArrayList<>();

	public RedisConfigPanel() {
		this(null);
	}

	public RedisConfigPanel(RedisConfigModel configModel) {
		// TODO Auto-generated constructor stub
	}

	protected <T extends JTextField> T regDirtyListener(T component) {
		component.getDocument().addDocumentListener(dirtyListener);
		return component;
	}

	public RedisConfigModel getCurrentState() {
		return new RedisConfigModel(null);
	}

	public boolean isDirty() {
		return dirty;
	}

	public void setDirty() {
		this.dirty = true;
		notifyListeners();
	}

	public void setNotDirty() {
		this.dirty = false;
		notifyListeners();
	}

	protected void notifyListeners() {
		Runnable[] listeners = dirtyListeners.toArray(new Runnable[0]);
		for (Runnable listener : listeners) {
			listener.run();
		}
	}

	public void registerDirtyListener(Runnable dirtyListener) {
		this.dirtyListeners.add(dirtyListener);
	}

	public void deregisterDirtyListeners() {
		this.dirtyListeners.clear();
	}
}
