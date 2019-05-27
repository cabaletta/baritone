# Concurrently

[![Travis Build Status](https://travis-ci.org/kimmobrunfeldt/concurrently.svg)](https://travis-ci.org/kimmobrunfeldt/concurrently) [![AppVeyor Build Status](https://ci.appveyor.com/api/projects/status/github/kimmobrunfeldt/concurrently?branch=master&svg=true)](https://ci.appveyor.com/project/kimmobrunfeldt/concurrently) *master branch status*

[![NPM Badge](https://nodei.co/npm/concurrently.png?downloads=true)](https://www.npmjs.com/package/concurrently)

Run multiple commands concurrently.
Like `npm run watch-js & npm run watch-less` but better.

![](docs/demo.gif)

**Features:**

* Cross platform (including Windows)
* Output is easy to follow with prefixes
* With `--kill-others` switch, all commands are killed if one dies
* Spawns commands with [spawn-command](https://github.com/mmalecki/spawn-command)

## Install

The tool is written in Node.js, but you can use it to run **any** commands.

```bash
npm install -g concurrently
```

or if you are using it from npm scripts:

```bash
npm install concurrently --save
```

## Usage

Remember to surround separate commands with quotes:
```bash
concurrently "command1 arg" "command2 arg"
```

Otherwise **concurrently** would try to run 4 separate commands:
`command1`, `arg`, `command2`, `arg`.

In package.json, escape quotes:

```bash
"start": "concurrently \"command1 arg\" \"command2 arg\""
```

NPM run commands can be shortened:

```bash
concurrently "npm:watch-js" "npm:watch-css" "npm:watch-node"

# Equivalent to:
concurrently -n watch-js,watch-css,watch-node "npm run watch-js" "npm run watch-css" "npm run watch-node"
```

NPM shortened commands also support wildcards. Given the following scripts in
package.json:

```javascript
{
    //...
    "scripts": {
        // ...
        "watch-js": "...",
        "watch-css": "...",
        "watch-node": "...",
        // ...
    },
    // ...
}
```

```bash
concurrently "npm:watch-*"

# Equivalent to:
concurrently -n js,css,node "npm run watch-js" "npm run watch-css" "npm run watch-node"

# Any name provided for the wildcard command will be used as a prefix to the wildcard
# part of the script name:
concurrently -n w: npm:watch-*

# Equivalent to:
concurrently -n w:js,w:css,w:node "npm run watch-js" "npm run watch-css" "npm run watch-node"
```

Good frontend one-liner example [here](https://github.com/kimmobrunfeldt/dont-copy-paste-this-frontend-template/blob/5cd2bde719654941bdfc0a42c6f1b8e69ae79980/package.json#L9).

Help:

```

Usage: concurrently [options] <command ...>

Options:

  -h, --help                       output usage information
  -V, --version                    output the version number
  -k, --kill-others                kill other processes if one exits or dies
  --kill-others-on-fail            kill other processes if one exits with non zero status code
  --no-color                       disable colors from logging
  -p, --prefix <prefix>            prefix used in logging for each process.
  Possible values: index, pid, time, command, name, none, or a template. Default: index or name (when --names is set). Example template: "{time}-{pid}"

  -n, --names <names>              List of custom names to be used in prefix template.
  Example names: "main,browser,server"

  --name-separator <char>          The character to split <names> on.
  Default: ",". Example usage: concurrently -n "styles,scripts|server" --name-separator "|" <command ...>

  -c, --prefix-colors <colors>     Comma-separated list of chalk colors to use on prefixes. If there are more commands than colors, the last color will be repeated.
  Available modifiers: reset, bold, dim, italic, underline, inverse, hidden, strikethrough
  Available colors: black, red, green, yellow, blue, magenta, cyan, white, gray
  Available background colors: bgBlack, bgRed, bgGreen, bgYellow, bgBlue, bgMagenta, bgCyan, bgWhite
  See https://www.npmjs.com/package/chalk for more information.
  Default: "gray.dim". Example: "black.bgWhite,cyan,gray.dim"

  -t, --timestamp-format <format>  specify the timestamp in moment/date-fns format. Default: YYYY-MM-DD HH:mm:ss.SSS

  -r, --raw                        output only raw output of processes, disables prettifying and concurrently coloring
  -s, --success <first|last|all>   Return exit code of zero or one based on the success or failure of the "first" child to terminate, the "last" child, or succeed  only if "all" child processes succeed. Default: all

  -l, --prefix-length <length>     limit how many characters of the command is displayed in prefix.
  The option can be used to shorten long commands.
  Works only if prefix is set to "command". Default: 10

  --allow-restart                  Restart a process which died. Default: false

  --restart-after <miliseconds>    delay time to respawn the process. Default: 0
  
  --restart-tries <times>          limit the number of respawn tries. Default: 1

  --default-input-target <identifier> identifier for child process to which input on stdin should be sent if not specified at start of input. Can be either the index or the name of the process. Default: 0

Input:

Input can be sent to any of the child processes using either the name or index
of the command followed by a colon. If no child identifier is specified then the
input will be sent to the child specified by the `--default-input-target`
option, which defaults to index 0.

Examples:

 - Kill other processes if one exits or dies

     $ concurrently --kill-others "grunt watch" "http-server"
     
 - Kill other processes if one exits with non zero status code

     $ concurrently --kill-others-on-fail "npm run build:client" "npm run build:server"

 - Output nothing more than stdout+stderr of child processes

     $ concurrently --raw "npm run watch-less" "npm run watch-js"

 - Normal output but without colors e.g. when logging to file

     $ concurrently --no-color "grunt watch" "http-server" > log

 - Custom prefix

     $ concurrently --prefix "{time}-{pid}" "npm run watch" "http-server"

 - Custom names and colored prefixes

     $ concurrently --names "HTTP,WATCH" -c "bgBlue.bold,bgMagenta.bold" "http-server" "npm run watch"
     
 - Shortened NPM run commands

     $ concurrently npm:watch-node npm:watch-js npm:watch-css

 - Send input to default

     $ concurrently "nodemon" "npm run watch-js"
     rs  # Sends rs command to nodemon process

 - Specify a default-input-target

     $ concurrently --default-input-target 1 "npm run watch-js" nodemon
     rs

 - Send input to specific child identified by index

     $ concurrently "npm run watch-js" nodemon
     1:rs

 - Send input to specific child identified by name

     $ concurrently -n js,srv "npm run watch-js" nodemon
     srv:rs

 - Send input to default

     $ concurrently "nodemon" "npm run watch-js"
     rs  # Sends rs command to nodemon process

 - Specify a default-input-target

     $ concurrently --default-input-target 1 "npm run watch-js" nodemon
     rs

 - Send input to specific child identified by index

     $ concurrently "npm run watch-js" nodemon
     1:rs

 - Send input to specific child identified by name

     $ concurrently -n js,srv "npm run watch-js" nodemon
     srv:rs

 - Shortened NPM run commands

     $ concurrently npm:watch-node npm:watch-js npm:watch-css

 - Shortened NPM run command with wildcard

     $ concurrently npm:watch-*

For more details, visit https://github.com/kimmobrunfeldt/concurrently
```

## FAQ

* Process exited with code *null*?

    From [Node child_process documentation](http://nodejs.org/api/child_process.html#child_process_event_exit), `exit` event:

    > This event is emitted after the child process ends. If the process
    > terminated normally, code is the final exit code of the process,
    > otherwise null. If the process terminated due to receipt of a signal,
    > signal is the string name of the signal, otherwise null.


    So *null* means the process didn't terminate normally. This will make **concurrent**
    to return non-zero exit code too.


## Why

I like [task automation with npm](http://substack.net/task_automation_with_npm_run)
but the usual way to run multiple commands concurrently is
`npm run watch-js & npm run watch-css`. That's fine but it's hard to keep
on track of different outputs. Also if one process fails, others still keep running
and you won't even notice the difference.

Another option would be to just run all commands in separate terminals. I got
tired of opening terminals and made **concurrently**.
