// credit scztt aka Scott Carver
// Env.fromArray solves IEnvGen problem

+Env {
	midicps {
		var copy;
		copy = this.copy;
		copy.levels = copy.levels.midicps;
		^copy
	}

	degreeToKey {
		|...args|
		var copy;
		copy = this.copy;
		copy.levels = copy.levels.degreeToKey(*args);
		^copy
	}

	performBinaryOp { arg aSelector, theOperand, adverb;
		var copy;
		if (theOperand.isKindOf(Env)) {
			^this.performBinaryOpOnEnv(aSelector, theOperand, adverb)
		} {
			copy = this.copy;
			copy.levels = theOperand.performBinaryOpOnSeqColl(aSelector, copy.levels, adverb);
			^copy
		}
	}

	performBinaryOpOnEnv { arg aSelector, theOperand, adverb;
		NotYetImplementedError("Env:performBinaryOpOnEnv").throw
	}

	performBinaryOpOnSomething { arg aSelector, theOperand, adverb;
		^this.performBinaryOp(aSelector, theOperand, adverb);
	}

	+ { arg other, adverb; ^other.performBinaryOp('+', this, adverb) }
	- { arg other, adverb; ^other.performBinaryOp('-', this, adverb) }
	* { arg other, adverb; ^other.performBinaryOp('*', this, adverb) }
	/ { arg other, adverb; ^other.performBinaryOp('/', this, adverb) }

	*fromArray {
		|array|
		var originalArray, size, expectedSize, releaseNode, loopNode,
		levelArray, timeArray, shapeArray;

		originalArray = array;
		array = array.reverse;

		levelArray = levelArray.add(array.pop());
		size = array.pop();

		expectedSize = ((size + 1) * 4);
		if (expectedSize.isNumber && (originalArray.size != expectedSize)) {
			Error("Array has incorrect size (expected %, actually %)".format(expectedSize, originalArray.size)).throw;
		};

		releaseNode = array.pop();
		if (releaseNode == -99) { releaseNode = nil };

		loopNode = array.pop();
		if (loopNode == -99) { loopNode = nil };

		while { array.notEmpty() } {
			var shapeIndex, curve;

			levelArray = levelArray.add(array.pop());
			timeArray = timeArray.add(array.pop());

			shapeIndex = array.pop();
			curve = array.pop();

			if (shapeIndex == 5) { // hard-coded to be a numeric curve
				shapeArray = shapeArray.add(curve)
			} {
				shapeArray = shapeArray.add(Env.shapeNames.findKeyForValue(shapeIndex) ?? 0)
			};
		};

		^Env(levelArray, timeArray, shapeArray, releaseNode, loopNode);
	}

	padTo {
		|size|
		levels = levels ++ (0 ! (1 + size - levels.size));
		times = times ++ (0 ! (size - times.size));
	}
}

+String {
	isValidUGenInput { ^this.asSymbol.isMap }
}

+Symbol {
	isValidUGenInput { ^this.isMap }
}