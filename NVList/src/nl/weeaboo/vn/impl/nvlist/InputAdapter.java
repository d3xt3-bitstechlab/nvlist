package nl.weeaboo.vn.impl.nvlist;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import nl.weeaboo.game.input.UserInput;
import nl.weeaboo.game.input.VKey;
import nl.weeaboo.io.EnvironmentSerializable;
import nl.weeaboo.lua.io.LuaSerializable;
import nl.weeaboo.vn.IInput;

@LuaSerializable
public class InputAdapter extends EnvironmentSerializable implements IInput {

	private final UserInput input;
	
	public InputAdapter(UserInput i) {
		input = i;
	}
	
	@Override
	public boolean consumeKey(int keycode) {
		return input.consumeKey(keycode);
	}

	@Override
	public boolean isKeyHeld(int keycode) {
		return input.isKeyHeld(keycode);
	}

	@Override
	public long getKeyHeldTime(int keycode) {
		return input.getKeyHeldTime(keycode);
	}
	
	@Override
	public boolean isKeyPressed(int keycode) {
		return input.isKeyPressed(keycode);
	}

	@Override
	public double getMouseX() {
		return input.getMouseX();
	}

	@Override
	public double getMouseY() {
		return input.getMouseY();
	}

	@Override
	public boolean consumeMouse() {
		return input.consumeMouse();
	}

	@Override
	public boolean isMouseHeld() {
		return input.isMouseHeld();
	}

	@Override
	public long getMouseHeldTime() {
		return input.getMouseHeldTime();
	}
	
	@Override
	public boolean isMousePressed() {
		return input.isMousePressed();
	}

	@Override
	public boolean consumeConfirm() {
		return consumeKey(KeyEvent.VK_ENTER)
			|| consumeKey(VKey.BUTTON2.toKeyCode(1));
	}

	@Override
	public boolean consumeCancel() {
		return consumeKey(KeyEvent.VK_ESCAPE)
			|| input.consumeMouse(MouseEvent.BUTTON3)
			|| consumeKey(VKey.BUTTON1.toKeyCode(1));
	}
	
	@Override
	public boolean consumeTextLog() {
		return consumeKey(KeyEvent.VK_LEFT)
			|| consumeKey(KeyEvent.VK_UP)
			;
	}
	
	@Override
	public boolean consumeTextContinue() {
		return consumeMouse()
			|| consumeKey(KeyEvent.VK_ENTER)
			|| consumeKey(KeyEvent.VK_RIGHT)
			|| consumeKey(KeyEvent.VK_DOWN)
			|| consumeKey(VKey.BUTTON2.toKeyCode(1));
	}

	@Override
	public boolean consumeEffectSkip() {
		return consumeTextContinue();
	}

	@Override
	public boolean consumeViewCG() {
		return consumeKey(KeyEvent.VK_SPACE);		
	}
	
	@Override
	public boolean consumeSaveScreen() {
		return consumeKey(KeyEvent.VK_S);
	}

	@Override
	public boolean consumeLoadScreen() {
		return consumeKey(KeyEvent.VK_L);
	}
	
	@Override
	public boolean isQuickRead() {
		return getMouseHeldTime() > 1000
			|| getKeyHeldTime(KeyEvent.VK_ENTER) > 2000
			|| getKeyHeldTime(VKey.BUTTON2.toKeyCode(1)) > 2000
			|| isKeyHeld(VKey.BUTTON3.toKeyCode(1))
			|| isKeyHeld(KeyEvent.VK_CONTROL);
	}
	
	@Override
	public boolean isEnabled() {
		return input.isEnabled();
	}

	@Override
	public void setEnabled(boolean e) {
		input.setEnabled(e);
	}
	
}
