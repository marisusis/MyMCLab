<h3>MyMCLab Config</h3>
This page describes the use of the plugin's config.yml file.<br>
Most things are already explained in the default config.yml, but this page is for a complete reference.
<hr><b>Before you start...</b>
Lists can be specified using 2 ways:
<b>Key:</b>
- 'item1'
- 'item2'
<b>Key: </b>['item1', 'item2']
They are both functional, but I recommend the first approach when using longer texts.
<hr><b>Password</b>
The password required for logging in to this server.
Groups can be used instead of this, which are quite a bit more customisable
<hr><b>Permission-Defaults</b>
A section containing all default values for permissions. The value can either be 'true' or 'false'
<b>Permission-Defaults:</b>
&nbsp;&nbsp;<b>default.input.commands:</b> false
A list of default permissions:
default.view.console, default.view.chat, default.view.errors, default.view.players,
 default.view.serverinfo, default.input.chat, default.input.commands, default.click.players, default.click.serverinfo
<hr><b>Groups</b>
A section, containing all groups specified by the user.
The children of this section must specify the group's name, which then also hold a section:
<b>Groups:</b>
&nbsp;&nbsp;<b>&lt;group name&gt;:</b>
&nbsp;&nbsp;&nbsp;&nbsp;<b>Key: </b>Value
Accepted in a group section:
<ul>
    <li><b>Password: </b> The group's password. If not specified this group is a 'parent' and cannot be logged into</li>
    <li><b>Permissions: </b>A list of permissions. Prefix a permission with a dash (-) to negate (disallow) it</li>
    <li><b>Parents: </b>A comma-separated list of parent group names. Note that this is not like a normal list,
    instead you can use <b>Parents: </b>'item1, item2, item3'
</ul>
<hr><b>Player-Info</b> and <b>Server-Info</b>
2 identically loaded configuration sections. Like the names suggest, Player-Info shows player info and Server-Info about the server.
This can be reached through the tab with a player head, and shows up when you click on the name of someone/click on "Server Info".
This is a List, yet every element is another section. It looks a bit like this:
<b>Player-Info:</b>
- <b>Key1: </b>Value1
&nbsp;&nbsp;<b>Key2: </b>Value2
- <b>Key1: </b>Value3
&nbsp;&nbsp;<b>Key2: </b>Value4
Notice how a dash (-) means a new entry of the list.
A breakdown of all the keys you can put here:
<ul>
    <li><b>Text: </b>The text to display on the item. Supports & colors and placeholders</li>
    <li><b>Type: </b>The type of the item, either 'none', 'progress-bar' or 'progress-circle'</li>
    <li><b>Value: </b>The progress of the item, if type is progress-related. Supports placeholders</li>
    <li><b>Max: </b>The max progress of the item. Displayed value is (value / max) * 100%. Supports placeholders too.</li>
    <li><b>Progress-Color: </b>The color of the item's progress, in hex. Supported formats: RGB, ARGB, RRGGBB, AARRGGBB where A is alpha, R is red, G is green and B is blue.
    See a <a href="http://www.colorpicker.com/">Color Picker</a> website for picking colors (hex at the top on this link)</li>
    <li><b>Empty-Color: </b>The color of the item's non-finished progress. Same input as Progress-Color</li>
    <li><b>Click-Commands: </b>A list of commands to execute on click, supports placeholders.</li>
    <li><b>Make-Toast: </b>A message that pops up to the client on click, supports placeholders.</li>
    <li><b>Prompts: </b>A section that holds values that are prompted to the client on click.
    If values are specified here, click-commands and make-toast will be executed after the client confirmed the prompt.
    An example:
    <b>Prompts:</b>
    &nbsp;&nbsp;<b>&lt;id&gt;:</b>
    &nbsp;&nbsp;&nbsp;&nbsp;<b>Key: </b>Value
    &nbsp;&nbsp;&nbsp;&nbsp;<b>Key2: </b>Value2
    You can then also use {prompt_&lt;id&gt;} in click-commands and make-toast to retrieve a prompt's value.
    Values you can put in here:
    <ul>
        <li><b>Type: </b>The type of the request. Either text, password, checkbox, number, decimal-number, selection or child</li>
        <li><b>Name: </b>The text to display to the client to describe the prompt.</li>
        <li><b>Checked and Unchecked: </b>Specifies the value to display if the type is <i>checkbox</i> and it's checked or unchecked.</li>
        <li><b>Values: </b>
        If type is <i>selection</i>, this is a list holding all possible options.
        If type is <i>child</i>, this is a section with keys as client input and the value as the result.
        Note that a child is not actually displayed to the client, and can be used to map results to responses.
        </li>
    </ul>
    </li>
</ul>