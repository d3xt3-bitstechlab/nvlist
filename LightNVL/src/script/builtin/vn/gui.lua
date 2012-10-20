-------------------------------------------------------------------------------
-- gui.lua
-------------------------------------------------------------------------------
-- User-interface related classes and functions
-------------------------------------------------------------------------------

module("vn.gui", package.seeall)

-- ----------------------------------------------------------------------------
--  Classes
-- ----------------------------------------------------------------------------

GUIComponent = {
	x=0,
	y=0,
	w=1,
	h=1
}

function GUIComponent.new(self)
	return extend(GUIComponent, self)
end

function GUIComponent:getX()
	return self.x
end

function GUIComponent:getY()
	return self.y
end

function GUIComponent:getWidth()
	return self.w
end

function GUIComponent:getHeight()
	return self.h
end

function GUIComponent:setPos(x, y)
	return self:setBounds(x, y, self:getWidth(), self:getHeight())
end

function GUIComponent:setSize(w, h)
	return self:setBounds(self:getX(), self:getY(), w, h)
end

function GUIComponent:setBounds(x, y, w, h)
	self.x = x
	self.y = y
	self.w = w
	self.h = h
end

function GUIComponent:getVisible()
	return self.visible
end

function GUIComponent:setVisible(v)
	self.visible = v
	if self.setAlpha ~= nil then
		if v then
			self:setAlpha(1)
		else
			self:setAlpha(0)
		end
	end
end

-- ----------------------------------------------------------------------------

-- Declares a button/text drawable hybrid
TextButton = {
	button=nil,
	text=nil,
	alpha=1
}

---Creates a new TextButton; a button with a text drawable on top.
-- @param background the image path to use for the button
-- @param text The text for the button label
function TextButton.new(background, text, self)
	self = GUIComponent.new(extend(TextButton, self))
	
	self.button = self.button or button(background)
	self.text = self.text or textimg(text)
	self.text:setAnchor(5)
	
	if self.button:getTexture() == nil then
		local w = self.text:getTextWidth()
		local h = self.text:getTextHeight()
		local pad = math.max(2, math.min(w, h) / 4)
		w = w + pad * 2
		h = h + pad * 2
		self.button:setNormalTexture(colorTex(0xC0000000, w, h))
		self.button:setRolloverTexture(colorTex(0xC0808080, w, h))
		self.button:setPressedTexture(colorTex(0xC0404040, w, h))
	end

	self:fitText()
	
	return self
end

function TextButton:destroy()
	self.button:destroy()
	self.text:destroy()
end

function TextButton:setZ(z)
	self.button:setZ(z)
	self:fitText()
end

function TextButton:getAlpha(a)
	return self.alpha
end

function TextButton:setAlpha(a)
	self.alpha = a
	self.button:setAlpha(a)
	self.text:setAlpha(a)
end

function TextButton:setSelected(s)
	self.button:setSelected(s)
end

function TextButton:fitText()
	local b = self.button
	local txt = self.text
	txt:setBounds(b:getX(), b:getY(), b:getWidth(), b:getHeight())
	txt:setZ(b:getZ() - 10)
end

function TextButton:getX()
	return self.button:getX()
end

function TextButton:getY()
	return self.button:getY()
end

function TextButton:getWidth()
	return self.button:getWidth()
end

function TextButton:getHeight()
	return self.button:getHeight()
end

function TextButton:setBounds(x, y, w, h)
	self.button:setBounds(x, y, w, h)
	self:fitText()
end

function TextButton:consumePress()
	return self.button:consumePress()
end

-- ----------------------------------------------------------------------------

-- Declares a button/text drawable hybrid
Viewport = {
	w=100,
	h=100,
	vw=0,
	vh=0,
	pad=0,
	alpha=1.0,
	children=nil,
	basePositions=nil,
	layer=nil,
	scrollWheelSpeed=8,
	snapInertia=.8,
	mouseSnap=nil,
	scrollThumb=nil,
	topEdge=nil,
	bottomEdge=nil,
	fadeEdgeLength=screenHeight*.03,
	scrollBar=nil,
	scrollThumb=nil,
	scrollBarWidth=screenHeight*.02,
	scrollBarPad=0,
	--Internal use
	lastMouseY=-1,
	scrollY=0,
	scrollYMin=0,
	scrollYMax=0,
	scrollSpeedY=0,
	snap=0,
}

local viewportLayerCount = 0

---Creates a new Viewport; a scrollable section of the screen containing other
-- Drawables or GUI components.
function Viewport.new(self)
	self = GUIComponent.new(extend(Viewport, self))		
		
	self.children = self.children or {}
	self.basePositions = self.basePositions or {}
	
	if self.mouseSnap == nil then
		if System.isLowEnd() then
			self.mouseSnap = .2
		else
			self.mouseSnap = .1
		end
	end
	
	viewportLayerCount = viewportLayerCount + 1
	self.layer = createLayer("viewport" .. viewportLayerCount)	
	
	return self
end

function Viewport:destroy()
	destroyValues(self.scrollThumb, self.topEdge, self.bottomEdge)
	destroyValues(self.children)	
	self.children = {}
	
	if self.layer ~= nil then
		self.layer:destroy()
		self.layer = nil
	end
end

function Viewport:getZ()
	return self.layer:getZ()
end

function Viewport:setZ(z)
	self.layer:setZ(z)
end

function Viewport:getInnerWidth(ignoreScrollBar)
	local iw = self:getWidth() - self.pad*2
	if not ignoreScrollBar then
		iw = iw - self.scrollBarPad*2 - self.scrollBarWidth
	end
	return iw
end

function Viewport:update()
	local snap = self.snap
	local my = self.lastMouseY

	local y = self.scrollY
	local dy = self.scrollSpeedY
	local minY = self.scrollYMin
	local maxY = self.scrollYMax
	local canScroll = (maxY > minY)

	local layoutNeeded = false
	
	local mouseX = input:getMouseX()
	local mouseY = input:getMouseY()
	local mouseContained = self.layer:contains(mouseX, mouseY)

	--Calculate mouse drag
	local dragging = input:isMouseHeld() and (my >= 0 or (mouseContained and input:consumeMouse()))
	if dragging and canScroll then
	    snap = self.mouseSnap        
	    
		if mouseY >= 0 then
			if my >= 0 then
				dy = my - mouseY
			end
			my = mouseY
		end
	else
		if dy ~= 0 then
			dy = dy * self.snapInertia
		end
	    
	    if mouseContained then
		    local mscroll = self.scrollWheelSpeed * input:getMouseScroll()
		    if mscroll ~= 0 then
		        snap = 1
		        dy = mscroll
		    end
		end
			    
		my = -1
	end	
	
	--Update and limit y
	if dy > -1 and dy < 1 then
		dy = 0
	else
		y = y + dy
		layoutNeeded = true
	end
	
	if not dragging or not canScroll then
		if y < minY then
			y = math.min(minY, y + (minY - y) * snap)
			if math.abs(y-minY) < 1 then
				y = minY
			end
			layoutNeeded = true
		elseif y > maxY then
			y = math.max(maxY, y + (maxY - y) * snap)
			if math.abs(y-maxY) < 1 then
				y = maxY
			end
			layoutNeeded = true
		end
	end
		
	self.snap = snap
	self.lastMouseY = my
	
	self.scrollY = y
	self.scrollSpeedY = dy
	
	if layoutNeeded then
		self:layout(true)
	end
end

function Viewport:layout(scrollOnly)
	local x = self:getX()
	local y = self:getY()
	local w = self:getWidth()
	local h = self:getHeight()
	self.layer:setBounds(x, y, w, h)

	local minY = 0
	local maxY = math.max(minY, self.vh - h)
	local scrollY = self.scrollY
	local scrollBarWidth = self.scrollBarWidth
	local scrollBarPad = self.scrollBarPad
	local canScroll = (maxY > minY)

	if not scrollOnly then		
		self:layoutChildren()
	end
	
	if self.topEdge ~= nil then
		local fl = math.min(self.fadeEdgeLength, scrollY-minY)
		self.topEdge:setBounds(0, 0, w, fl)
	end
	if self.bottomEdge ~= nil then
		local fl = math.min(self.fadeEdgeLength, maxY-scrollY)
		self.bottomEdge:setBounds(0, h-fl+1, w, fl)
	end
	
	for i,c in ipairs(self.children) do
		local newX = self.basePositions[i].x
		local newY = self.basePositions[i].y - scrollY
		local bottomY = newY + c:getHeight()
		c:setPos(newX, newY)
		if bottomY < 0 or newY > h then
			c:setVisible(false)
		else
			c:setVisible(true)
		end		
	end

	local scrollBar = self.scrollBar
	local scrollThumb = self.scrollThumb
	
	if not scrollOnly and scrollBar ~= nil then
		if canScroll then
			scrollBar:setAlpha(1)		
			scrollBar:setSize(scrollBarWidth, h-scrollBarPad*2)
			scrollBar:setPos(w-scrollBarPad-scrollBarWidth, scrollBarPad)	
		else
			scrollBar:setAlpha(0)
		end
	end
	
	if scrollThumb ~= nil then
		if canScroll then
			scrollThumb:setAlpha(1)		
		else
			scrollThumb:setAlpha(0)
		end
		local scrollFrac = math.max(0, math.min(1, (scrollY-minY)/math.abs(maxY-minY)))
		local stx = scrollBar:getX() + (scrollBar:getWidth()-scrollThumb:getWidth())/2
		local sty = scrollBar:getY() + (scrollBar:getHeight()-scrollThumb:getHeight()) * scrollFrac
		scrollThumb:setPos(stx, sty)
	end
		
	self.scrollYMin = minY
	self.scrollYMax = maxY
end

function Viewport:layoutChildren()
	local basepos = self.basePositions
	for i,c in ipairs(self.children) do	
		c:setPos(basepos[i].x, basepos[i].y)
	end
end

function Viewport:openLayer()
	setImageLayer(self.layer)
end

function Viewport:closeLayer(components, edgeInfo, scrollInfo)
	local pathPrefix = "gui/components#"
	if android then
		pathPrefix = "android/components#"
	end

	destroyValues(self.topEdge, self.bottomEdge)
		edgeInfo = edgeInfo or {}
	if edgeInfo == false then
		self.topEdge = nil
		self.bottomEdge = nil
	else	
		self.topEdge = img(edgeInfo.top or pathPrefix .. "fade-down",
			extend({z=-999, colorRGB=0}, edgeInfo.topExtra))
		self.bottomEdge =  img(edgeInfo.bottom or pathPrefix .. "fade-up",
			extend({z=-999, colorRGB=0}, edgeInfo.bottomExtra))
	end
	
	destroyValues(self.scrollBar, self.scrollThumb)
	scrollInfo = scrollInfo or {}
	local scrollBarWidth = scrollInfo.scrollBarWidth or self.scrollBarWidth
	local scrollBarPad = scrollInfo.scrollBarPad or self.scrollBarPad
	local scrollThumbWidth = scrollInfo.scrollThumbWidth or scrollBarWidth
	if scrollInfo == false then
		self.scrollBar = nil
		self.scrollThumb = nil
	else	
		local scrollThumb = img(scrollInfo.thumb or pathPrefix .. "scroll-thumb",
			extend({z=-1002}, scrollInfo.thumbExtra))    
		local scrollThumbScale = scrollThumbWidth / scrollThumb:getUnscaledWidth()
		scrollThumb:setScale(scrollThumbScale, scrollThumbScale)
		self.scrollThumb = scrollThumb
		
	    local scrollBar = img(scrollInfo.bar or pathPrefix .. "scroll-bg",
	    	extend({z=-1001}, scrollInfo.barExtra))
		self.scrollBar = scrollBar		
	end
	self.scrollBarWidth = scrollBarWidth
	self.scrollBarPad = scrollBarPad
	
	setImageLayer(nil)
	
	for _,c in ipairs(components) do
		table.insert(self.children, c)
	end
	self:layoutVirtual()
	self:layout(false)
end

function Viewport:layoutVirtual()
	local pad = self.pad
	local vw = 0
	local vh = 0
	for i,c in ipairs(self.children) do
		self.children[i] = c
		local cx = c:getX()
		local cy = c:getY()
		self.basePositions[i] = {x=cx+pad, y=cy+pad}
		vw = math.max(vw, cx+c:getWidth()+pad*2)
		vh = math.max(vh, cy+c:getHeight()+pad*2)		
	end
	self.vw = vw
	self.vh = vh
end

function Viewport:scrollTo(frac)
	self.scrollY = self.scrollYMin + frac * (self.scrollYMax - self.scrollYMin)	
	self:layout(true)
end

-- ----------------------------------------------------------------------------

Layout = {
	children=nil
}

function Layout.new(self)
	self = GUIComponent.new(extend(Layout, self))
	self.children = self.children or {}
	return self
end

function Layout:add(d)
	table.insert(self.children, d)
end

function Layout:remove(d)
	removeAll(self.children, d)
end

function Layout:layout()
	for _,c in pairs(self.children) do
		c:setSize(self.w, self.h)
		c:setPos(self.x, self.y)
	end
end

-- ----------------------------------------------------------------------------

GridLayout = {
	cols=-1,
	pad=0,	
	fillW=false,
	fillH=false,
	pack=0
}

---Creates a new grid layout object, layouts out its subcomponents in
-- equal-sized cells.
function GridLayout.new(self)
	self = Layout.new(extend(GridLayout, self))
	return self
end

function GridLayout:layout()
	local cols = self.cols
	local rows = 1
	if cols > 0 then
		rows = math.ceil(#self.children / cols)
	else
		cols = #self.children
	end
	
	local pad = self.pad
	local w = self.w
	local h = self.h
	
	local colW = (w-(cols-1)*pad) / cols
	local rowH = (h-(rows-1)*pad) / rows
	local maxColW = 0
	local maxRowH = 0
	for _,d in ipairs(self.children) do
		if self.fillW or self.fillH then
			local targetW = colW
			if not self.fillW then
				targetW = d:getWidth()
			end
			local targetH = rowH
			if not self.fillH then
				targetH = d:getHeight()
			end
			d:setSize(targetW, targetH)
		end
		if self.pack then
			maxColW = math.max(maxColW, d:getWidth())
			maxRowH = math.max(maxRowH, d:getHeight())
		end
	end
		
	local startX = self.x
	local startY = self.y		
	if self.pack > 0 then
		colW = math.min(colW, maxColW)
		rowH = math.min(rowH, maxRowH)
		startX = startX + alignAnchorX(w, cols*(colW+pad)-pad, self.pack)
		startY = startY + alignAnchorY(h, rows*(rowH+pad)-pad, self.pack)
	end
	
	local x = startX
	local y = startY
	local c = 0
	for _,d in ipairs(self.children) do
		d:setPos(x + alignAnchorX(colW, d:getWidth(), 5),
				 y + alignAnchorY(rowH, d:getHeight(), 5))
	
		c = c + 1
		x = x + colW + pad
		if cols >= 0 and c >= cols then
			c = 0
			x = startX
			y = y + rowH + pad
		end
	end
end


-- ----------------------------------------------------------------------------

FlowLayout = {
	pack=7,
	pad=0,
	cols=-1
}

---Creates a new flow layout object, layouts its components in a line.
function FlowLayout.new(self)
	self = Layout.new(extend(FlowLayout, self))
	return self
end

function FlowLayout:layoutLine(components, x, y, pack, pad, width)
	if components == nil or #components == 0 then
		return 0
	end
	
	--Determine line height
	local lw = math.max(0, #components - 1) * pad
	local lh = 0
	for _,c in ipairs(components) do
		lw = lw + c:getWidth()
		lh = math.max(lh, c:getHeight())
	end
	
	--Layout components
	x = x + alignAnchorX(width, lw, pack)
	for _,c in ipairs(components) do
		c:setPos(x, y + alignAnchorY(lh, c:getHeight(), pack))		
		x = x + pad + c:getWidth()
	end	
	
	return lh
end

function FlowLayout:layout()
	local pad = self.pad
	local x = self.x + pad
	local y = self.y + pad
	local cols = self.cols
	
	local lineComponents = {}
	local lineSize = 0
	local maxLineSize = self.w - pad*2
	for _,c in ipairs(self.children) do
		local size = c:getWidth()
		if lineSize + pad + size > maxLineSize or (cols > 0 and #lineComponents >= cols) then
			if #lineComponents > 0 then
				y = y + pad + self:layoutLine(lineComponents, x, y, self.pack, pad, maxLineSize)
				lineComponents = {}
			end
			lineSize = 0
		end
		table.insert(lineComponents, c)
	end
	y = y + pad + self:layoutLine(lineComponents, x, y, self.pack, pad, maxLineSize)
end

-- ----------------------------------------------------------------------------
--  Functions
-- ----------------------------------------------------------------------------

---Returns the correct X offset for a component with width <code>inner</code>
-- given the width <code>outer</code> of its desired container.
-- @param outer Width of the container
-- @param inner Width of the component inside the container
-- @param anchor Alignment of the component within the container. These
--        correspond to numpad directions (4=left, 5=center, 6=right).
-- @return The correct X offset for the inner component
function alignAnchorX(outer, inner, anchor)
	if anchor == 2 or anchor == 5 or anchor == 8 then
		return (outer-inner) / 2
	elseif anchor == 3 or anchor == 6 or anchor == 9 then
		return (outer-inner)
	end
	return 0		
end

---Returns the correct Y offset for a component with height <code>inner</code>
-- given the height <code>outer</code> of its desired container.
-- @param outer Height of the container
-- @param inner Height of the component inside the container
-- @param anchor Alignment of the component within the container. These
--        correspond to numpad directions (8=top, 5=center, 2=bottom).
-- @return The correct Y offset for the inner component
function alignAnchorY(outer, inner, anchor)
	if anchor >= 4 and anchor <= 6 then
		return (outer-inner) / 2
	elseif anchor >= 1 and anchor <= 3 then
		return (outer-inner)
	end
	return 0		
end
