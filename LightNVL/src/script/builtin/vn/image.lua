---Functions to show and manipulate images on the screen.
--  
module("vn.image", package.seeall)

-- ----------------------------------------------------------------------------
--  Variables
-- ----------------------------------------------------------------------------

local imageStateMeta = {
	{background=nil, layer=nil}
}

-- ----------------------------------------------------------------------------
--  Functions
-- ----------------------------------------------------------------------------

---Returns the <code>x, y, z</code> of the specified sprite slot
-- <code>(lc, l, c, rc, r)</code>
-- @param i The image to position
-- @param slot The sprite slot (a string)
-- @param y The baseline y (sprite bottom y-coordinate)
-- @return Three values, the natural <code>x, y, z</code> for the image in the
--         specified sprite slot.
local function getSpriteSlotPosition(i, slot, y)
	local x = 0
	local y = (y or screenHeight) - i:getHeight()
	local z = 0
	
	local w2 = i:getWidth()/2
	if slot == "l" then
		x = screenWidth*1/5 - w2
		z = 1
	elseif slot == "lc" then
		x = screenWidth*1/3 - w2
		z = 2		
	elseif slot == "c" then
		x = screenWidth*1/2 - w2
	elseif slot == "rc" then
		x = screenWidth*2/3 - w2
		z = 2		
	elseif slot == "r" then
		x = screenWidth*4/5 - w2
		z = 1
	end
	
	return x, y, z
end

---Creates an image and adds it to the current image layer.
-- @param tex A texture object or a path to a valid image file (relative to
--        <code>res/img</code>).
-- @param x Can be either a string or a number. If it's a number, it specifies
--        the leftmost x-coordinate of the image. If it's a string, it can be
--        one of:<br/>
--        <ul>
--          <li>l</li>
--          <li>lc</li>
--          <li>c</li>
--          <li>rc</li>
--          <li>r</li>
--        </ul>
--        These refer to predefined sprite positions from left to right.
-- @param y If x is given as a string, the desired y-coordinate of the bottom
--        of the sprite. If x is a number, the topmost y-coordinate. 
-- @param properties Optional argument containing a table containing initial values
--        for the new image's properties. 
-- @treturn ImageDrawable The newly created image.
function img(tex, x, y, properties)
	if type(x) == "table" then
		properties = x
	elseif type(y) == "table" then
		properties = y
	end

	local i = Image.createImage(getImageLayer(), tex)
		
	if type(x) == "string" then
		local z = 0
		x, y, z = getSpriteSlotPosition(i, x, y)
		i:setZ(z)
	end	
	if type(x) == "number" and type(y) == "number" then
		i:setPos(x, y)
	end
	
	--Handle properties given in a table
	if type(properties) == "table" then
		for k,v in pairs(properties) do
			setProperty(i, k, v)	
		end
	end	
		
	return i
end

---Like <code>img</code>, gradually fades in the new image instead of instantly
-- displaying it.
-- @param tex A texture object or a path to a valid image file (relative to
--        <code>res/img</code>).
-- @param x Can be either a string or a number. If it's a number, it specifies
--        the leftmost x-coordinate of the image. If it's a string, it can be
--        one of:<br/>
--        <ul>
--          <li>l</li>
--          <li>lc</li>
--          <li>c</li>
--          <li>rc</li>
--          <li>r</li>
--        </ul>
--        These refer to predefined sprite positions from left to right.
-- @param y If x is given as a string, the desired y-coordinate of the bottom
--        of the sprite. If x is a number, the topmost y-coordinate. 
-- @param properties Optional argument containing a table with overrides for
--        the new image's properties. 
-- @treturn ImageDrawable The newly created image.
-- @see img
function imgf(tex, x, y, properties)
	local i = img(tex, x, y, properties)
	i:setAlpha(0)
	fadeTo4(i, 1)
	return i	
end

---Destroys an image or drawable, removing it from the screen.
-- @param image The image to remove.
function rm(image)
	if image ~= nil and not image:isDestroyed() then
		image:destroy()
	end
end

---Like <code>rm</code>, but fades out the image gradually before destroying it.
-- @param image The image to remove.
-- @number fadeTimeFrames The duration of the fade-out effect in frames.
-- @see rm
function rmf(image, fadeTimeFrames)
	fadeTo4(image, 0, fadeTimeFrames)
	rm(image)
end

---Changes the current background image.
-- @param tex A texture object or a path to a valid image file (relative to
--        <code>res/img</code>).
-- @tab properties Optional argument containing a table with overrides for
--      the new image's properties. 
-- @treturn ImageDrawable The new background image.
function bg(tex, properties)
	local background = getBackground()
    if background ~= nil and not background:isDestroyed() then
        background:destroy()
    end
    
    properties = extend({z=30000}, properties or {})
	background = img(tex, properties)
	
	setImageStateAttribute("background", background)	
	return background
end

---Like <code>bg</code>, but gradually fades to the new background instead of
-- instantly changing it.
-- @param tex A texture object or a path to a valid image file (relative to
--        <code>res/img</code>).
-- @number[opt=30] fadeTimeFrames The duration of the fade-out effect in frames.
-- @tab[opt={}] properties Optional argument containing a table with overrides
--              for the new image's properties. 
-- @treturn ImageDrawable The new background image.
-- @see bg
function bgf(tex, fadeTimeFrames, properties)
	fadeTimeFrames = fadeTimeFrames or 30

	local background = getBackground()
	if background == nil or background:isDestroyed() then	
		background = bg(tex, properties)
		if fadeTimeFrames > 0 then
			background:setAlpha(0)
			fadeTo4(background, 1, fadeTimeFrames)
		end
	else
		local newbg = img(tex, properties)
		if fadeTimeFrames > 0 then		
			newbg:setAlpha(0)
			newbg:setZ(background:getZ() - 1)
			fadeTo4(newbg, 1, fadeTimeFrames)
		end
		newbg:setZ(background:getZ())		
	    background:destroy()
	    background = newbg
	end	
		
	setImageStateAttribute("background", background)
	return background
end

---Returns the current background image.
-- @treturn ImageDrawable The current background image, or <code>nil</code> if
--          no background image currently exists.
function getBackground()
	local imageLayer = getImageLayer()
	local background = getImageStateAttribute("background")
	if imageLayer == nil or not imageLayer:contains(background) then
		setImageStateAttribute("background", nil)
		background = nil
	end
	return background
end

---Replaces the current background with the <code>bg</code>.
-- @tparam ImageDrawable bg The new background image.
function setBackground(bg)
	local old = getImageStateAttribute("background")
	if old ~= nil and old ~= bg then
	    rmbg()
	end
	setImageStateAttribute("background", bg)		
end

---Removes and destroys the background image previously created with
-- <code>bg</code>.
function rmbg()
	local bg = getBackground()
	setImageStateAttribute("background", nil)
	if bg == nil then
		return
	end
	return rm(bg)
end

---Like <code>rmbg</code>, but fades out the background image gradually before
-- destroying it.
-- @number fadeTimeFrames The duration of the fade-out effect in frames.
-- @see rmbg
function rmbgf(fadeTimeFrames)
	local bg = getBackground()
	setImageStateAttribute("background", nil)
	if bg == nil then
		return
	end
	return rmf(bg, fadeTimeFrames)
end

---Creates a texture object from an image file. 
-- @param filename The path to a valid image file (relative to
--        <code>res/img</code>). When a texture is passed instead of a filename,
--        the function will just return that texture.
-- @bool[opt=false] suppressErrors If <code>true</code> suppresses any errors
--                  that occur during loading.
-- @treturn Texture The created Texture object, or <code>nil</code> if something
--          went wrong.  
function tex(filename, suppressErrors)
	if type(filename) == "string" then
		return Image.getTexture(filename, suppressErrors)
	end
	return filename
end

---Creates a texture object with the specified color.
-- @param argb The ARGB color packed into a single int (<code>0xFFFF0000</code>
--        is red, <code>0xFF0000FF</code> is blue, etc.)
-- @int w The width for the generated texture.
-- @int h The height for the generated texture.
-- @treturn Texture A new texture (w,h) with all pixels colored
--          <code>argb</code>.
function colorTex(argb, w, h)
	return Image.createColorTexture(argb, w, h)
end

---Creates a new on-screen button.
-- @string filename Path to an image file (relative to <code>res/img</code>).
-- @treturn ButtonDrawable The newly created button.
function button(filename)
	return Image.createButton(getImageLayer(), filename)
end

---Creates a new TextDrawable, used to display dynamic text on the screen.
-- @string[opt=""] text The initial text to display.
-- @treturn TextDrawable The newly created text drawable.
function textimg(text)
	return Image.createText(getImageLayer(), text)
end

---Creates a new layer.
-- @tparam Layer parentLayer The parent layer for the new layer.
-- @treturn Layer The newly created layer.
function createLayer(parentLayer)
	parentLayer = parentLayer or getImageLayer()
	return Image.createLayer(parentLayer)
end

---Creates a new camera object.
-- @tparam Layer layer The layer to create the camera on.
-- @treturn Camera The newly created camera object.
function createCamera(layer)
	layer = layer or getImageLayer()
	return Image.createCamera(layer)
end

---Takes a screenshot to be used later (usually to create an ImageDrawable by
-- passing the screenshot to <code>img</code>).
-- @tparam Layer layer The layer in which to take the screenshot. Any layers
--        underneath it will be visible in the screenshot. Passing
--        <code>nil</code> for this parameter takes a screenshot of all layers.
-- @int[opt=-999] z The z-index in the selected layer to take the screenshot at.
-- @bool[opt=true] clip If <code>false</code>, ignores the layer's clipping
--                 bounds.
-- @bool[opt=false] volatile Allow optimizations which may cause the
--                  screenshot's pixels to disappear at any time.
-- @treturn Screenshot A screenshot object to be used as an argument for the
--          <code>img</code> function later.
function screenshot(layer, z, clip, volatile)
	local ss = nil
	while ss == nil do
		ss = Image.screenshot(layer, z, clip, volatile)
		while not ss:isAvailable() and not ss:isCancelled() do
			--print("looping", ss:isCancelled())
			yield()
		end
        
		if not ss:isAvailable() then
			ss = nil 
		end
	end
	return ss	
end

---Takes a screenshot and makes an image out of it. Very useful for creating
-- complex fade effects by making it possible to fade out the entire screen
-- as a single image.
-- @tparam Layer layer The layer in which to take the screenshot. Any layers
--        underneath it will be visible in the screenshot. Passing
--        <code>nil</code> for this parameter takes a screenshot of all layers.
-- @int[opt=-999] z The z-index in the selected layer to take the screenshot at.
-- @bool[opt=true] clip If <code>false</code>, ignores the layer's clipping
--                 bounds.
-- @bool[opt=false] volatile Allow optimizations which may cause the
--                  screenshot's pixels to disappear at any time.
-- @treturn ImageDrawable The image created from the screenshot.
function screen2image(layer, z, clip, volatile)
	layer = layer or getImageLayer()
	z = z or -999
	
	i = Image.createImage(layer, screenshot(layer, z, clip, volatile))
	i:setZ(z + 1)
	return i
end

---Gradually changes the alpha of <code>i</code> to <code>targetAlpha</code>.
-- @tparam Drawable i The image to change the alpha of.
-- @number targetAlpha The end alpha for <code>i</code>.
-- @number durationFrames The duration of the movement in frames (gets
--         multiplied with <code>effectSpeed</code> internally)
function fadeTo(i, targetAlpha, durationFrames)
	durationFrames = durationFrames or 20
	
	local startAlpha = i:getAlpha()
	local frame = 1
	while frame + effectSpeed <= durationFrames do
		local f = frame / durationFrames
		i:setAlpha(startAlpha + (targetAlpha - startAlpha) * f)
		frame = frame + effectSpeed
		yield()
	end
	
    i:setAlpha(targetAlpha)
end

fadeTo4 = fadeTo

---Gradually moves <code>i</code> to <code>(x, y)</code>.
-- @tparam Drawable i The image to move.
-- @number x The end x-position for <code>i</code>
-- @number y The end y-position for <code>i</code>
-- @number durationFrames The duration of the movement in frames (gets
--         multiplied with <code>effectSpeed</code> internally)
-- @tparam Interpolator interpolator A function or interpolator object mapping
--         an input in the range <code>(0, 1)</code> to an output in the range
--         <code>(0, 1)</code>.
function translateTo(i, x, y, durationFrames, interpolator)
	x = x or i:getX()
	y = y or i:getY()
	durationFrames = durationFrames or 60
	interpolator = Interpolators.get(interpolator, Interpolators.SMOOTH)
		
	local startX = i:getX()
	local startY = i:getY()
	
	local frame = 1
	while not i:isDestroyed() and frame + effectSpeed <= durationFrames do
		local f = interpolator:remap(frame / durationFrames)
		i:setPos(startX + (x-startX) * f, startY + (y-startY) * f)
		frame = frame + effectSpeed
		yield()
	end
	i:setPos(x, y)
end

---Gradually moves <code>i</code> by <code>(dx, dy)</code>, relative to its
-- current position.
-- @tparam Drawable i The image to move.
-- @number dx The end x-position for <code>i</code>
-- @number dy The end y-position for <code>i</code>
-- @number durationFrames The duration of the movement in frames (gets
--         multiplied with <code>effectSpeed</code> internally)
-- @tparam Interpolator interpolator A function or interpolator object mapping
--         an input in the range <code>(0, 1)</code> to an output in the range
--         <code>(0, 1)</code>.
function translateRelative(i, dx, dy, durationFrames, interpolator)
	if i == nil then
		i = {}
	elseif type(i) ~= "table" then
		i = {i}
	end
	dx = dx or 0
	dy = dy or 0

	local threads = {}
	for _,d in pairs(i) do
		table.insert(threads, newThread(translateTo, d, d:getX()+dx, d:getY()+dy, durationFrames, interpolator))
	end		
	join(threads)
end

---Asks NVList to preload one or more images. In most cases NVList does a pretty
-- good job preloading images by itself, but in rare cases a little helpcan
-- improve performance.
-- tparam string ... Any number of filenames of images to preload.
function preload(...)
	return Image.preload(...)
end

---Changes the current image layer. Functions that create Drawables such as
-- <code>img</code> typically create them in the image layer.
-- @tparam Layer layer The layer to use as image layer.
function setImageLayer(layer)
	setImageStateAttribute("layer", layer)
end

---Returns the current image layer.
-- @treturn Layer The current image layer.
-- @see setImageLayer
function getImageLayer()
	local layer = getImageStateAttribute("layer")
	if layer == nil or layer:isDestroyed() then
		layer = imageState:getDefaultLayer()
		setImageStateAttribute("layer", layer)
	end
	return layer
end

---Returns the overlay layer which lies on top of (most) other layers and can
-- be used for effects that need to cover most things on the screen, including
-- the text box.
-- @treturn Layer The overlay layer.
function getOverlayLayer()
	return imageState:getOverlayLayer()
end

---Returns the root layer which (recursively) contains all other layers.
-- @treturn Layer The root layer.
function getRootLayer()
	return imageState:getRootLayer()
end

---Stores a copy of the current layers, then restores the image state to the
-- initial set of layers. 
function pushImageState()
	imageState:push() --pushLayerState()
	
	table.insert(imageStateMeta, {textMode = getTextMode()})
	setTextMode(0)
end

---The inverse of <code>pushImageState</code>, restores the layers to the way
-- they were when <code>pushImageState</code> was last used. 
-- @see pushImageState
function popImageState()
	setTextMode(0)
	imageState:pop() --popLayerState()
	
	local t = table.remove(imageStateMeta)
	setTextMode(t.textMode)
end

---Sets an attribute that gets pushed/popped together with the image state when
-- <code>pushImageState</code>/<code>popImageState</code> is called.
-- @string key The name of the attribute to set.
-- @param val The new value to store for the given name.
function setImageStateAttribute(key, val)
	local meta = imageStateMeta[#imageStateMeta]
	meta[key] = val
end

---Returns the value of an attribute stored with
-- <code>setImageStateAttribute</code>.
-- @string key The name of the attribute to get the value of.
-- @return The current value of the attribute specified by <code>key</code>.
-- @see setImageStateAttribute
function getImageStateAttribute(key)
	local meta = imageStateMeta[#imageStateMeta]
	return meta[key]
end

---Saves the contents of a layer, restore the contents with
-- <code>popLayerState</code> at a later time. After the layer's contents have
-- been saved with this function, the layer is completely cleared.
-- @tparam Layer layer The layer to save the contents of.
-- @int[opt=nil] z If specified, doesn't clear drawables with
--               <code>drawable.z &lt;= z</code> after saving the layer's
--               contents.
function pushLayerState(layer, z)
	layer = layer or getImageLayer()
	if z ~= nil then
		layer:push(z)
	else
		layer:push()
	end
end

---Restores the layer's contents, as previously saved with
-- <code>pushLayerState</code>.
-- @see pushLayerState
function popLayerState(layer)
	layer = layer or getImageLayer()
	layer:pop()
end

---Enters CG view mode, which temporarily hides the text box until the text
-- continue key is pressed.
function viewCG()
	local textLayer = getTextLayer()
	local textDrawable = textState:getTextDrawable()
	if getTextMode() == 0
		or textLayer == nil or textLayer:isDestroyed() or not textLayer:isVisible(.001)
		or textDrawable == nil or textDrawable:isDestroyed() or not textDrawable:isVisible(.001)
	then
		notifier:message("Text box is already invisible")
		return
	end	

	local ss0 = screenshot(getRootLayer(), -32768)
	local ss1 = screenshot(getRootLayer(), -1999)
	
	local textLayer = getTextLayer()
	return setMode("viewCG", function()
		pushImageState()
		local i0 = img(ss0)
		local i1 = imgf(ss1, {z=-1})
	
		while not input:consumeCancel() and not input:consumeConfirm()
			and not input:consumeTextContinue() and not input:consumeViewCG() do
			
			yield() 
		end
		
		rmf(i1)
		rm(i0)
		setMode(nil)
	end, function()
		popImageState()
	end)
end
