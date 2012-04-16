require("builtin/stdlib")
require("builtin/vn")

vn.flattenModule(_G)

function main()
	return titlescreen()
end

function titlescreen()
    globals:clear()
	setTextModeADV()
        
    textoff(1)
    bgf("gui/titlescreen-background")
    local startB = button("gui/titlescreen-buttons#start-")
    local loadB  = button("gui/titlescreen-buttons#load-")
    local extraB = button("gui/titlescreen-buttons#extra-")
    local quitB  = button("gui/titlescreen-buttons#quit-")

    local buttons = {startB, loadB, extraB, quitB}
    local y = (screenHeight - 100 * #buttons) / 2
    for i,b in pairs(buttons) do
        local startY = y + 100 * (i-1)
        local endY = startY + 100
        b:setPos(screenWidth-300, startY+(endY-startY-b:getHeight())/2)
    end
    
    while true do
        if startB:consumePress() then
            destroyValues(buttons) --Removes the buttons
            rmbgf()
            texton(1)
            --return call("game/op.lvn")
            return call("tutorial.lvn")
        elseif loadB:consumePress() then
            loadScreen()
        elseif extraB:consumePress() then
            notifier:message("Extra button pressed")
        elseif quitB:consumePress() then
            exit(false)
        end
        yield()
    end
    
    exit(true)
end
