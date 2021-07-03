# LayoutMode
LayoutMode helpes to organize CSS rules for resposive website.

[![NPM version][npm-image]][npm-url] [![Downloads][downloads-image]][npm-url]

# Introduction
LayoutMode allows CSS rules to be organized into modes which are in turned controlled by media query.
In the nute shell, LM decouple media query and the CSS rules.
With LM, media queries can be created to select named modes.
The modes are, then, translated to classes of body element.
The actualy style CSS rules can then be tided to the modes of the body. 

Before

{MediaQuery} ==> {CSS rules}

With LM

{MediaQuery} ==> {modes (body.mode-XXX)} ==> {CSS rules}

As the mode appears the class on the body element, you can use that as a qualifier to style anything under body ... and that is everything.

You can also have multiple mode at the same time as they are independent.
For example, you may have 'compact' and 'spacious' for different resoluation and also have 'normal-contrast' and 'high-contrast' for font size and color scheme.
Then at one time, the mode might be `['spacious', 'high-contrast']`.


## DEMO
Try the above out here [Live demo on jsFiddle](https://jsfiddle.net/wnf9e968/14/ "Live demo on jsFiddle!")


# How to use
First you need to include the library to your page. See below for CDN or NPM options.

Initiate the modes using the name you think reperesenting the look you want and it can be use in both conversation and code.

    <script src='layout-mode.min.js'>
    ...
    var layoutMode = LayoutMode.init(['mobile', 'desktop', 'ie'])

You can specify the default mode (the mode that is not subject to any media query) using ...

    body > .css-layout-mode-selector > div.mode-mobile {
        display: block;
    }

The following code will cause the mode to change to desktop mode when the width is 800px or wider

    @media(min-width: 800px) {
        body > .css-layout-mode-selector > div.mode-mobile { display: none; }
        body > .css-layout-mode-selector > div.mode-desktop { display: block; }
    }

That is when the page is 800px or narrower the body element will have 'mode-mobile' class and it will have 'mode-desktop' class when the page is wider than 800px.

# Why use LayoutMode

## Mode names represent intentions
Each mode has a name associated with it.
This is far different from having to interpret the media queries and the CSS rules.
The team members can use the name to communication.
The names such as 'mobile', 'desktop', 'reader' modes directly communicate what sort of look you are trying to achieve.

**Jack:** Jill, I think the header text has too much padding on my phone, can you fix that.

**Jill:** One second.

    bode.mode-mobile h1 {
        padding: 0px;
    }

**Jill**: Done!

## Current mode
You can check exactly what the current mode is.

**Adam:** Why?! Why the menu bar still appear, the screen is very narrow already. It should change to hidden by now with this width.

**Ben:** Why don't you check what is the current mode is.

**Adam:** Right, let me open the developer tool

    console.log(layoutMode.currentModes())

**Adam:** Umm. It is the right mode. Why the menu is not ... Oh wait .. I spell 'position' wrong on this rule.

With one more line of code, you can also tell LayoutMode to display the current mode.

    <link rel="stylesheet" type="text/css" href="layout-mode.min.css">

By default, with the above mode, the current mode name is shown on the left-buttom corner of the screen.

## Notify when mode change
You can ask LayoutMode to notify when the mode has changed.
This is useful if you need to programmatically do something when mode change from one to another.

    layoutMode.watch(function(modes) {
        console.log(modes)
    })

## Decouple MediaQuery and CSS Rules
Putting the mode in between, the media queries decoupled from the rules.
That means you do not need to make the media to fit the query to see the effect of the rules.
You can ask LayoutMode to change the mode programmatically.

    layoutMode.changeModes(['compact'])

with above mode, media queries are detached from the modes and only response to programatically change of modes. To re-attach the media query to the mode, use `clearModes`.

    layoutMode.clearModes()

## Separation of works
A team member can be given responsibility to ensure the proper media queries for selecting modes.
Other team members can also work independently on different modes.
With the names clearly spell out on the rules, it is harder to step on each other's toes.
You can even go further and separate css files for each modes.

## NPM
    npm install layoutmode

## CDN
### Non-Minified
- https://cdn.rawgit.com/NawaMan/LayoutMode/master/layout-mode.js
- https://cdn.rawgit.com/NawaMan/LayoutMode/master/layout-mode.css

### Minifie
- https://cdn.rawgit.com/NawaMan/LayoutMode/master/layout-mode.min.js
- https://cdn.rawgit.com/NawaMan/LayoutMode/master/layout-mode.min.css


## Links
### GitHub
https://github.com/NawaMan/LayoutMode
### GitHub Page
https://nawaman.github.io/LayoutMode/
### NPM page
https://www.npmjs.com/package/layoutmode
## DEMO
https://jsfiddle.net/wnf9e968/14/

[npm-url]: https://npmjs.org/package/layoutmode
[downloads-image]: http://img.shields.io/npm/dm/layoutmode.svg
[npm-image]: http://img.shields.io/npm/v/layoutmode.svg
