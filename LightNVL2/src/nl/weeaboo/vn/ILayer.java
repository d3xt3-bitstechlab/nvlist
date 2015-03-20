package nl.weeaboo.vn;

import java.io.Serializable;

import nl.weeaboo.game.entity.Entity;
import nl.weeaboo.vn.image.IScreenshotBuffer;

public interface ILayer extends IDestructible, IEntityContainer, IRenderable, Serializable {

    /**
     * Creates a new entity and adds it to this layer.
     */
    public Entity createEntity();

	/**
	 * @return {@code true} if the specified layer is a descendant of this layer.
	 */
	public boolean containsLayer(ILayer layer);

	/**
	 * @return A buffer for pending screenshots. Screenshots requests queued in this buffer will be fullfilled
	 *         at some later time.
	 */
	public IScreenshotBuffer getScreenshotBuffer();

	public void setX(double x); //Calls setPos
	public void setY(double y); //Calls setPos
	public void setZ(short z);
	public void setWidth(double w); //Calls setSize
	public void setHeight(double h); //Calls setSize
	public void setPos(double x, double y);
	public void setSize(double w, double h);

	/** Simultaneously sets the size and pos of this layer */
	public void setBounds(double x, double y, double w, double h);

	public void setVisible(boolean v);

}
