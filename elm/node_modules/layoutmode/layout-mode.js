/* LayoutMode v0.0.1, @license MIT */
(function(global, factory) {
    typeof define === 'function' && define.amd ? define(factory) :
        (global.LayoutMode = factory());
}(this, (function() {
    'use strict';

    var _propsVarName = '_propsValues'
    var $ = function(sel) { return document.querySelectorAll(sel) }
    var listen = window.addEventListener
    var toJsonstr = JSON.stringify
    var isFunction = function(thing) { return typeof thing === 'function' }

    var LayoutMode = {
        init: function(pConfigureOrAllModes, pModeClassPrefix, pSelectorClass, pModeDivPrefix) {
            var bodyClassList =  document.body.classList

            var isConfig = pConfigureOrAllModes.constructor !== Array
            var config   = isConfig ? pConfigureOrAllModes : {}
            var allModes        = (isConfig ? config.modes         : pConfigureOrAllModes)
            var modeClassPrefix = (isConfig ? config.classPrefix   : pModeClassPrefix) || 'mode-'
            var selectorClass   = (isConfig ? config.selectorClass : pSelectorClass)   || 'css-layout-mode-selector'
            var modeDivPrefix   = (isConfig ? config.divPrefix     : pModeDivPrefix)   || 'mode-'

            var listeners = {}
            var selector  = 'body > .' + selectorClass

            var retriveModes = function() {
                var modes = []
                var modeDivs = $(selector + ' div')
                for (var i = 0; i < modeDivs.length; i++) {
                    var modeDiv = modeDivs[i]
                    if (getComputedStyle(modeDiv, null).display !== 'none') {
                        var foundMode = modeDiv.textContent
                        if (modes.indexOf(foundMode) == -1)
                            modes.push(foundMode)
                    }
                }
                modes.sort()
                return modes
            }
            var currentModes = function() {
                var modes = []
                for (var i = 0; i < bodyClassList.length; i++) {
                    var clazz = bodyClassList[i]
                    if (clazz.indexOf(modeClassPrefix) === 0)
                        modes.push(clazz.substr(modeClassPrefix.length))
                }
                
                modes.sort()
                return modes
            }
            var updateMode = function() {
                var retrives = retriveModes()
                var currents = currentModes()
                var isDiff
                    = ((currents.length != retrives.length)
                    || ((currents.length != 0) && (currents[0] !=retrives[0])
                    || (toJsonstr(currents) != toJsonstr(retrives))))

                if (!isDiff)
                    return false
                
                for (var i = 0; i < currents.length; i++)
                    bodyClassList.remove(modeClassPrefix + currents[i])
                
                for (var i = 0; i < retrives.length; i++)
                    bodyClassList.add(modeClassPrefix + retrives[i])

                currents = currentModes()
                for (var name in listeners)
                    listeners[name](currents)
            }
            var changeModes = function(modes) {
                var modeDivs = $(selector + ' div')
                for (var i = 0; i < modeDivs.length; i++) {
                    var modeDiv = modeDivs[i]
                    var foundMode = modeDiv.textContent
                    modeDiv.style.display = modes.includes(foundMode) ? 'block' : 'none'
                }

                updateMode()
            }
            var dispatchEvent = window.dispatchEvent
            var clearModes = function(modes) {
                var modeDivs = $(selector + ' div')
                for (var i = 0; i < modeDivs.length; i++) {
                    var modeDiv = modeDivs[i]
                    modeDiv.style.display = ''
                }

                // forceViewportEvaluation
                if(isFunction(Event)) {
                    dispatchEvent(new Event('resize'));
                } else {
                    var evt = document.createEvent('UIEvents'); 
                    evt.initUIEvent('resize', true, false, window, 0); 
                    dispatchEvent(evt);
                }
            }
            var watch = function(nicknameOrCallback, callbackOnly) {
                var nickName = isFunction(nicknameOrCallback) ? 'nn' + Math.random() : nicknameOrCallback
                var callback = isFunction(callbackOnly)       ? callbackOnly         : nicknameOrCallback
                listeners[nickName] = callback

                return self
            }
            var unwatch = function(nicknameOrCallback) {
                var theNickname = null
                if (typeof nicknameOrCallback === 'string')
                    theNickname = nicknameOrCallback
                else {
                    var theNickname = null
                    for (var nickname in listeners) {
                        if (callback === nicknameOrCallback) {
                            theNickname = nickname
                        }
                    }
                }

                if (!theNickname)
                    delete listeners[theNickname]

                return self
            }

            var writeSelector = function(write) {
                var outterDiv = document.createElement('div')
                outterDiv.classList.add(selectorClass)
                for (var i = 0; i < allModes.length; i++) {
                    var mode = allModes[i]
                    var innerDiv = document.createElement('div')
                    innerDiv.classList.add(modeDivPrefix + mode)
                    innerDiv.innerText = mode
                    outterDiv.appendChild(innerDiv)
                }
                var styleDiv = document.createElement('style')
                styleDiv.innerText
                    = 'body > .' + selectorClass + ' { visibility: hidden; } \n'
                    + 'body > .' + selectorClass + ' > div { display: none; }\n'
                outterDiv.appendChild(styleDiv)

                document.body.appendChild(outterDiv)
            }

            writeSelector()
            listen("DOMContentLoaded", updateMode)
            listen("load",   updateMode)
            listen('resize', updateMode)
            updateMode()

            var self = {
                currentModes:currentModes,
                changeModes:changeModes,
                clearModes:clearModes,
                selector:selector,
                watch:watch,
                unwatch:unwatch,
                updateMode:updateMode
            }
            return self
        }
    }

    return LayoutMode;
})));