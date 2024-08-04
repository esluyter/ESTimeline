// this is just a wrapper because Event does not reliably make a compile string with keys in the same order (i.e. identical Events don't necessarily make identical CSs)

ESEvent : Event {
  asCompileString {
    var cs = "ESEvent.newFrom((";
    this.keys.asArray.sort({ |a, b| a.asString < b.asString }).do { |key|
      cs = cs ++ key.asCompileString ++ ": " ++ this[key].asCompileString ++ ", ";
    };
    cs = cs ++ "))";
    ^cs;
  }
}