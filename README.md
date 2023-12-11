### stringhold: A java.lang.String holder

# stringhold

Concatenating large Strings in Java, potentially with deep hierarchies, with inputs from different sources (local or network), etc., can be unnecessarily complex:

- Even with StringBuilders, text gets copied around multiple times
- Character buffers get reallocated over and over
- A missing input fragment blocks the assembly of the full output
- The output is written to a stream, so why did we assemble it to String in the first place?
- The output is being assembled and eventually we realize it's too long
- We don't realize we ever only need the first line of the 1 megabyte output, but we still create the full thing, involving complex computations
- and so on

**stringhold** comes to the rescue.

Stringhold provides classes replacing or improving the functionality of the following typical Java classes:

- String
- CharSequence
- Reader
- Appendable
- StringBuilder
- StringBuffer
- Writer

## StringHolder API

A `StringHolder` is something *holding* data that may turn into a String. This can simply be a String *itself*, something that can *assemble* a String from its own state, or something that gets a String *supplied* from somewhere else (e.g., a `Supplier<String>`).

The `StringHolder` is a `CharSequence`, and calling `#toString()` will yield the string it holds. All other methods, like `#length()`, `#charAt`, `#subSequence` etc. work the way you expect it. In addition, you can all `#toReader()` to wrap the contents as a `java.io.Reader`, and you can call `#appendTo(Appendable)` to write the contents to some other `Appendable`.

If you want to concatenate several `StringHolders`, very much like `StringBuilder`, you can use a `StringHolderSequence`, which by itself also is an `Appendable`, so you can also append Strings directly.

You can use a `Reader` source as a supplying source through the `ReaderStringHolder` subclass.

You can also build your own StringHolder subclass, if you want to.

## The Twist

All the operations listed above may in fact never create a String or concatenate them. If all that you need is to copy data from a Reader to a Writer, there is no need to construct a single String from it!

**stringhold** takes great care of delaying the materialization of String instances from input data as much as possible.

## Relaxing assumptions

Often, a criterion such as "too long" doesn't require to know the exact length of something. It's enough that we know we've hit a certain minimum length.

Also, when estimating buffer sizes, a minimum is handy, but so is an *expected length*, even though that may not be as clearly defined as a minimum.

Lastly, sometimes we know the exact length but not the content.

`StringHolder` instances can use this information for typical operations such as:

- `#equals(Object)` checks (if a minimum length of the other object exceeds our own length, it cannot be a match).
- `#isEmpty()` checks (a minimum length of 1 is sufficient to rule that out)

## Exceptions and error conditions

**stringhold** can handle situations where the Java API does not expect an `IOException` to be thrown, but it may still arise from within some code, for example, when assembling a String from a `Supplier` or `Reader` for use in `toString()` or when appending somewhere else.

The exception can either be wrapped in an `IllegalStateException` or the output can be augmented (e.g., just flushed, replaced with an empty string if possible, or appending the error message or the full stack trace for debugging).

In addition, `StringHolder` supports a `checkError()` method to indicate that some constraints were violated.

## StringHolderScope

A set of StringHolders can be associated with a `StringHolderScope`, which receives callbacks upon events regarding these `StringHolder` instances. Such scopes can be used to implement quota checks and error listeners, for example.

## Modern Java

**stringhold** is fully compatible with Java 8 and above. Thanks to a Multirelease jar structure, it can optimize the use of Java methods introduced in newer Java releases whenever possible, such as `CharSequence#isEmpty()` — without complicating its own public API.

## Code Quality

This project currently maintains a [100% code coverage](https://kohlschutter.github.io/stringhold/stringhold-codecoverage/jacoco-aggregate/index.html) policy.

This is one of the projects where this makes actual sense.

The **stringhold** API is expected to not have breaking changes across minor and patch releases. Changes in behavior are possible but should be clearly marked in the changelog.

# Usage Examples

## The Basics

A StringHolder using a String directly:

	StringHolder h = StringHolder.withContent("Some string");
	h.isString(); // always true

	h.toString(); // simply returns the associated string.

A StringHolder using a String supplier:

	StringHolder h = StringHolder.withSupplier(() -> "Some string");
	h.isString(); // not true yet

	h.toString(); // calls Supplier.get()
	h.isString(); // now true

	h.toString(); // doesn't Supplier.get() a second time, string is cached

A StringHolder using a String supplier and some length constraints:

	StringHolder h = StringHolder.withSupplierMinimumAndExpectedLength(5, 20, () -> "Some string");
	h.isEmpty(); // false because we claim the string is at least 5 characters long
	h.isString(); // not true yet

A StringHolder that reads contents from a function that supplies StringReader instances:

	StringHolder h = StringHolder.withReaderSupplier(() -> new StringReader("hello"), (
        e) -> IOExceptionHandler.ExceptionResponse.ILLEGAL_STATE);

A sequence of String(Holder)s, some are nested:

	StringHolderSequence seq = StringHolder.newSequence();
	seq.append("Hello");
	seq.append(' ');
	seq.append(StringHolder.newSequence().append(StringHolder.withSupplier(() -> "World"));

	// Append to a writer
	try (Writer out = new FileWriter(new File("/tmp/out"))) {
		seq.appendTo(out);
	}

	seq.toString(); // "Hello World"

A sequence of String(Holder)s that can be assembled/appended out of order via scatter-gatter (speed up overall assembly time through parallelization):

	StringHolderSequence seq = StringHolder.newAsyncSequence();
	seq.append(veryComplexStringHolder);
	seq.append(anotherStringHolder);
	
	// seq.toString() / seq.appendTo() may internally serialize the second StringHolder first.
	// The final order is still guaranteed to be correct.
	// This may save time at the cost of constructing temporary StringBuilders.

You can also define a conditional StringHolder, which may be defined but later excluded if a condition is not met:

    StringHolder seq = StringHolder.withContent("Hello", //
	   StringHolder.withConditionalStringHolder(StringHolder.withContent(" World"), (o) -> {
 	     return checkIfIncluded(o); // false: exclude; true: include
	   }));
	seq.toString() // returns "Hello" or "Hello World", depending on checkIfIncluded


## The full API

* [API JavaDoc](https://kohlschutter.github.io/stringhold/stringhold-common/apidocs/index.html)
* [Source code](https://kohlschutter.github.io/stringhold/stringhold-common/xref/index.html)
* [Test code](https://kohlschutter.github.io/stringhold/stringhold-common/xref-test/index.html)
* [Code coverage](https://kohlschutter.github.io/stringhold/stringhold-codecoverage/jacoco-aggregate/index.html)
* [Project website](https://kohlschutter.github.io/stringhold/)

## Benchmarks

TBD

## Future Improvements

- Add optimizations for the `IntStream` API

## Frequently Asked Questions

Q: Is this an accidental implementation of half of Lisp?

A: Maybe.

## Installation

### Maven

Add the following dependency:

    <dependency>
        <groupId>com.kohlschutter.stringhold</groupId>
        <artifactId>stringhold-common</artifactId>
        <version>1.0.0</version>
    </dependency>

### Gradle

    dependencies {
        implementation 'com.kohlschutter.stringhold:stringhold-common:1.0.0'
    }

## Building from source

You currently need Maven or Eclipse with m2e, and Java 15 or later. Just run:

	mvn clean install

To reformat code, which simplifies pull requests and restores general sanity, use:

	mvn process-sources -Preformat

# Changelog

### _(2023-12-11)_ **stringhold 1.0.1*

 - Fix liqp dependency

### _(2023-12-10)_ **stringhold 1.0.0**

 - Initial release

# Legal Notices

Copyright 2022, 2023 Christian Kohlschuetter <christian@kohlschutter.com>

SPDX-License-Identifier: Apache-2.0
See NOTICE and LICENSE for license details.
