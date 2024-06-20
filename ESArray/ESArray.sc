+ Object {
  asESArray {
    ^this.asCompileString;
  }

  *fromESArray { |arr|
    var stack = [];

    if (arr.isString) {
      ^arr.interpret;
    };

    arr.do { |item|
      if (item == $() {
        stack = stack.add((type: \class, args: []));
      };
      if (item.class.class == Class) {
        stack.last[\class] = item;
      };
      if (item == $[) {
        stack = stack.add((type: \array, args: []));
      };
      if (item.class == String) {
        stack.last[\args] = stack.last[\args].add(item.interpret);
      };
      if ((item == $]) or: (item == $))) {
        var ev = stack.pop;
        if (stack.size == 0) {
          stack = stack.add((type: \result, args: []));
        };
        switch (ev[\type])
        { \class } {
          stack.last.args = stack.last.args.add(ev[\class].new(*ev[\args]));
        }
        { \array } {
          stack.last.args = stack.last.args.add(ev[\args]);
        };
      };
    };
    ^stack.last.args[0];
  }
}

+ Array {
  asESArray {
    ^[$[, this.collect(_.asESArray), $]].flatIf(_.isString.not); ///ugg .flat makes strings chars
  }
}

+ ESTimeline {
  asESArray {
    ^[$(, this.class, this.storeArgs.collect(_.asESArray), $)].flatIf(_.isString.not);
  }
}

+ ESTrack {
  asESArray {
    ^[$(, this.class, this.storeArgs.collect(_.asESArray), $)].flatIf(_.isString.not);
  }
}

+ ESClip {
  asESArray {
    ^[$(, this.class, this.storeArgs.collect(_.asESArray), $)].flatIf(_.isString.not);
  }
}