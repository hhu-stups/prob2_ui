-- Based on https://stackoverflow.com/a/49396058 (CC-BY-SA 4.0)

local extensions_by_format = {
	["html"] = ".html",
	["asciidoc"] = ".adoc",
}

function Link(el)
	el.target = string.gsub(el.target, "%.md", extensions_by_format[FORMAT])
	return el
end
