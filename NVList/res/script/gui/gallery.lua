-------------------------------------------------------------------------------
-- gallery.lua
-------------------------------------------------------------------------------
-- Default image gallery.
-------------------------------------------------------------------------------


-- ----------------------------------------------------------------------------
--  Classes
-- ----------------------------------------------------------------------------

local ImageSlot = {
	index=-1,
    thumbnail=nil,
    fullpath=nil,
	button=nil,
    x=0,
    y=0,
    w=0,
    h=0
	}

function ImageSlot.new(self)
	self = extend(ImageSlot, self)
	
	self.button = button()
	self.button:setNormalTexture(tex(self.thumbnail or self.fullpath))
	self.button:setToggle(true)

	self.overlay = img("gui/imagegallery#white")
    self.overlay:setZ(0)
	self.overlay:setBlendMode(BlendMode.ADD)
	
	return self
end

function ImageSlot:destroy()
	self.button:destroy()
end

function ImageSlot:getWidth()
    return self.w
end

function ImageSlot:getHeight()
    return self.h
end

function ImageSlot:setPos(x, y)
    self.x = x
    self.y = y
    self:layout()
end

function ImageSlot:setSize(w, h)
    self.w = w
    self.h = h
    self:layout()
end

function ImageSlot:layout()
    local x = self.x
    local y = self.y
    local w = self.w
    local h = self.h    

	local b = self.button
	local scale = math.min(w / b:getUnscaledWidth(), h / b:getUnscaledHeight())
	
	b:setScale(scale, scale)		
	b:setPos(x + (w-b:getWidth())/2, y + (h-b:getHeight())/2)
	
	local o = self.overlay
	o:setBounds(b:getX(), b:getY(), b:getWidth(), b:getHeight())
	o:setZ(b:getZ() - 10)
end

function ImageSlot:update()
	if self.button:isRollover() and self.button:isEnabled() then
		self.overlay:setAlpha(0.5)
	else
		self.overlay:setAlpha(0)
	end
end

function ImageSlot:show()
    local path = self.fullpath or self.thumbnail
    preload(path) --Gives us a 2 frame head start for loading the full image.

    self.button:setEnabled(false)
    self.overlay:setAlpha(0)
    yield()
    
	local ss = screenshot()
	pushImageState()
	
	ss = img(ss)
	ss:setZ(1000)

	local i = img(path)

    local b = self.button
    local small = {b:getX(), b:getY(), b:getWidth(), b:getHeight()}
    local iw = math.max(1, i:getWidth())
    local ih = math.max(1, i:getHeight())
    local scale = math.min(screenWidth/iw, screenHeight/ih) --Find scale factor to fill screen, maintaining aspect ratio
    local large = {(screenWidth-scale*iw)/2, (screenHeight-scale*ih)/2, scale*iw, scale*ih} --Scaled image bounds, centered on the screen
    local dur = 20
    local ip = Interpolators.SMOOTH

    Anim.par({
        Anim.createTween(i, "bounds", small, large, dur, ip),
        Anim.createTween(ss, "alpha", nil, 0, dur)
    }):run()

    while true do
        if input:consumeCancel() or input:consumeConfirm() or input:consumeTextContinue() then
            break
        end
        yield()
    end

    Anim.par({
        Anim.createTween(i, "bounds", large, small, dur, ip),
        Anim.createTween(ss, "alpha", nil, 1, dur)
    }):run()
		
	i:destroy()
	ss:destroy()
	
	popImageState()
    self.button:setEnabled(true)
end

-- ----------------------------------------------------------------------------

local ImageGallery = {
	files=nil,
	slots=nil,
	pageButtons=nil,
	returnButton=nil,
	topFade=nil,
	bottomFade=nil,
	page=0,
	selected=0,
	rows=2,
	cols=3,
	pageButtonLayout=nil,
	slotLayout=nil,
	buttonBarLayout=nil,
	}

function ImageGallery.new(folder, self)
	self = extend(ImageGallery, self or {})
	
	self.files = self.files or Image.getImageFiles(folder)    
	local numPages = math.ceil(#self.files / (self.rows * self.cols))
	
	local buttonStyle = createStyle{fontName="sans serif", fontStyle="bold", shadowColor=0}
		
	self.pageButtons = self.pageButtons or {}
	for p=1,numPages do
		local tb = TextButton.new("gui/imagegallery#pageButton-", p)
		tb.text:setDefaultStyle(buttonStyle)
		tb.button:setToggle(true)
		table.insert(self.pageButtons, tb)
	end
	
	self.slots = self.slots or {}
	self.returnButton = TextButton.new("gui/imagegallery#button-", "Return")
	self.returnButton.text:setDefaultStyle(buttonStyle)
	
	self.topFade = img("gui/imagegallery#fade-top")
	self.topFade:setZ(10)

	self.bottomFade = img("gui/imagegallery#fade-bottom")
	self.bottomFade:setZ(10)
	
	self:setPage(1)
		
	return self
end

function ImageGallery:destroy()
    destroyValues(self.pageButtons)
    destroyValues(self.slots)
	destroyValues{self.returnButton}
end

function ImageGallery:layout()
	local w = screenWidth
	local h = screenHeight
	local ipad = w / 32
	local vpad = h / 7
	local quicksaveH = vpad / 2
	local mainW = w - ipad*2
    local mainPadV = h / 5.33333333333
	local mainH = h - mainPadV*2

	self.pageButtonLayout = GridLayout.new{w=w, h=vpad, pad=ipad, pack=5, children=self.pageButtons}
	self.pageButtonLayout:layout()
	self.slotLayout = GridLayout.new{x=ipad, y=mainPadV, w=mainW, h=mainH, cols=self.cols, pad=0, pack=5,
		children=self.slots, fillW=true, fillH=true}
	self.slotLayout:layout()
	self.buttonBarLayout = GridLayout.new{y=h-vpad, w=w, h=vpad, pad=ipad, pack=5, children={self.returnButton}}
	self.buttonBarLayout:layout()
		
	self.topFade:setBounds(0, 0, w, vpad)
	self.bottomFade:setBounds(0, math.ceil(h-vpad), w, vpad)
end

---Returns <code>true</code> if <code>fullpath</code> shouldn't be included in the image gallery
function ImageGallery:isExcludePath(fullpath)
    if string.sub(fullpath, -6) == "-thumb" then
        --Exclude files ending in "-thumb" (these are the thumbnail versions)
        return true
    end
    return false
end

---Returns the thumbnail Texture (or <code>nil</code> if no path exists) for the specified full image path.
function ImageGallery:getThumbnail(fullpath)
    local path = string.gsub(fullpath, "^(.*)%..-$", "%1") .. "-thumb" --Strip file-ext, append "-thumb"
    --print(path)
    return tex(path, true) --Try to retrieve the texture, suppressing any error encountered
end

function ImageGallery:setPage(p)
	for i,pb in ipairs(self.pageButtons) do
		pb.button:setSelected(i == p) 
	end

	if self.page ~= p then
		self.page = p
		
		--Destroy old slots
        destroyValues(self.slots)
		self.slots = {}				
		
		--Create new slots
		local slotsPerPage = self.rows * self.cols
		local pageStart = 1 + (p - 1) * slotsPerPage
		local pageEnd = math.min(#self.files, p * slotsPerPage)
		
		for i=pageStart,pageEnd do
			local index = i
			local fullpath = "gui/imagegallery#locked"
            local thumbnail = nil
			local empty = true
            
            local filename = self.files[i]
            filename = string.gsub(filename, "^(.*)%..-$", "%1") --Strips file-ext
			if seenLog:hasImage(filename) and not self:isExcludePath(filename) then
				empty = false
				fullpath = filename
                thumbnail = self:getThumbnail(filename)
            else 
                --print("Locked: ", filename)
			end
			
			local is = ImageSlot.new{index=index, thumbnail=thumbnail, fullpath=fullpath}
			is.button:setEnabled(not empty)
			table.insert(self.slots, is)
		end
		
		if self.selected < pageStart or self.selected >= pageEnd then
			self.selected = 0
		end
		self:setSelected(self.selected)
		
		self:layout()
	end
end

function ImageGallery:setSelected(i)
	self.selected = i
	
	local selectedSlot = nil
	for _,slot in ipairs(self.slots) do
		if slot.index == i then
			selectedSlot = slot			
			slot.button:setSelected(true)
		else
			slot.button:setSelected(false)		
		end
	end
	
	if selectedSlot ~= nil then
		selectedSlot:show()
	end
end

function ImageGallery:run()
	self:layout()

	while not input:consumeCancel() do
		for i,pb in ipairs(self.pageButtons) do
			if pb.button:consumePress() then
				self:setPage(i)
			end
		end
		for _,slot in ipairs(self.slots) do
			slot:update()
			if slot.button:consumePress() then
				self:setSelected(slot.index)
				break
			end
		end
		
		if self.returnButton.button:consumePress() then
			break
		end
		
		yield()
	end
end

-- ----------------------------------------------------------------------------
--  Functions
-- ----------------------------------------------------------------------------

function preloadImageGallery()
	preload("gui/imagegallery")
end

function imageGallery(folder, overrides)
	folder = folder or ""

    --Create a screenshot and place it over the screen, change images, then fade away screenshot
	local ss = screenshot()
	pushImageState()
	ss = img(ss)
	ss:setZ(-32000)
	local thread = newThread(rmf, ss)
	
	local gallery = ImageGallery.new(folder, overrides)
	gallery:run()	

    join(thread)

    --Create a screenshot and place it over the screen, change images, then fade away screenshot
	local ss = screenshot()
	gallery:destroy()	
	popImageState()
	ss = img(ss)
	ss:setZ(-32000)
	fadeTo(ss, 0)
	ss:destroy()	
end
