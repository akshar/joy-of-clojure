(ns ch9)

;;The choice of one namespace-creation mechanism over another amounts to choosing a level of control over the default symbolic mappings.

;;Using the ns macro automatically conveys two sets of symbolic mappings—all classes in the java.lang package and all the functions, macros, and special forms in the clojure.core namespace


;;Using the in-ns function imports the java.lang package like ns, but it doesn’t create any mappings for functions or macros in clojure.core


;;The finest level of control for creating namespaces is provided through the create-ns function, which when called takes a symbol and returns a namespace object: 

(def b (create-ns 'bonobo)) ;; create-ns only create java class mappings

b ;; => #namespace[bonobo]


((ns-map b) 'String) ;; => java.lang.String

;;When given a namespace object (also retrieved using the find-ns function), you can manipulate its bindings programmatically using the functions intern and ns-unmap:

(intern b 'x 9) ;; => #'bonobo/x

bonobo/x;; => 9


(intern b 'reduce clojure.core/reduce)


(intern b '+ clojure.core/+) ;;=> #'bonobo/+

(in-ns 'bonobo)

(reduce + [1 2 3 4 5])

;;=> 15

(in-ns 'user)
(get (ns-map 'bonobo) 'reduce)

(remove-ns 'bonobo)
(all-ns) ;; returns all-namespace
;; => (#namespace[nrepl.middleware.interruptible-eval]
;;     #namespace[cider.nrepl.pprint]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.info]
;;     #namespace[cider.nrepl.middleware.track-state]
;;     #namespace[clojure.stacktrace]
;;     #namespace[leiningen.core.classpath]
;;     #namespace[leiningen.core.eval]
;;     #namespace[cider.nrepl.middleware.util.nrepl]
;;     #namespace[leiningen.core.pedantic]
;;     #namespace[cider.nrepl.inlined-deps.cljs-tooling.v0v3v1.cljs-tooling.util.analysis]
;;     #namespace[leiningen.repl]
;;     #namespace[clojure.test]
;;     #namespace[cider.nrepl.inlined-deps.toolsreader.v1v3v2.clojure.tools.reader]
;;     #namespace[cider.nrepl.middleware.content-type]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources.special-forms]
;;     #namespace[dynapath.util]
;;     #namespace[cider.nrepl.middleware.debug]
;;     #namespace[clojure.core.server]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.java]
;;     #namespace[clojure.core.specs.alpha]
;;     #namespace[nrepl.server]
;;     #namespace[pjstadig.util]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.namespace]
;;     #namespace[nrepl.middleware.session]
;;     #namespace[cider.nrepl.inlined-deps.toolsnamespace.v0v3v1.clojure.tools.namespace.dependency]
;;     #namespace[clojure.reflect]
;;     #namespace[cider.nrepl.inlined-deps.dynapath.v1v0v0.dynapath.defaults]
;;     #namespace[cider.nrepl.middleware.inspect]
;;     #namespace[ch9]
;;     #namespace[cider.nrepl.middleware.util.error-handling]
;;     #namespace[nrepl.middleware.caught]
;;     #namespace[cider.nrepl.inlined-deps.toolsreader.v1v3v2.clojure.tools.reader.default-data-readers]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.java.classpath]
;;     #namespace[leiningen.trampoline]
;;     #namespace[cider.nrepl.inlined-deps.toolsnamespace.v0v3v1.clojure.tools.namespace.parse]
;;     #namespace[clojure.spec.alpha]
;;     #namespace[leiningen.core.project]
;;     #namespace[clojure.set]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.inspect]
;;     #namespace[cider.nrepl.inlined-deps.suitable.v0v2v14.suitable.ast]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.cljs.analysis]
;;     #namespace[cider.nrepl.middleware.complete]
;;     #namespace[cemerick.pomegranate.aether]
;;     #namespace[cider.nrepl.middleware.stacktrace]
;;     #namespace[nrepl.ack]
;;     #namespace[clojure.java.browse]
;;     #namespace[clojure.string]
;;     #namespace[classlojure.core]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.util.os]
;;     #namespace[clojure.java.javadoc]
;;     #namespace[cider.nrepl.inlined-deps.toolsreader.v1v3v2.clojure.tools.reader.impl.inspect]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.spec]
;;     #namespace[clojure.repl]
;;     #namespace[cider.nrepl.inlined-deps.cljs-tooling.v0v3v1.cljs-tooling.info]
;;     #namespace[clojure.template]
;;     #namespace[cider.nrepl.inlined-deps.javaclasspath.v0v3v0.clojure.java.classpath]
;;     #namespace[nrepl.misc]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.meta]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources.resources]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.java.legacy-parser]
;;     #namespace[leiningen.update-in]
;;     #namespace[cider-nrepl.plugin]
;;     #namespace[cider.nrepl.inlined-deps.dynapath.v1v0v0.dynapath.dynamic-classpath]
;;     #namespace[cider.nrepl.inlined-deps.toolsreader.v1v3v2.clojure.tools.reader.reader-types]
;;     #namespace[cider.nrepl.inlined-deps.toolsnamespace.v0v3v1.clojure.tools.namespace.file]
;;     #namespace[clojure.core]
;;     #namespace[clojure.walk]
;;     #namespace[cider.nrepl.inlined-deps.dynapath.v1v0v0.dynapath.util]
;;     #namespace[nrepl.middleware]
;;     #namespace[dynapath.defaults]
;;     #namespace[cider.nrepl.inlined-deps.toolsreader.v1v3v2.clojure.tools.reader.impl.utils]
;;     #namespace[cider.nrepl.inlined-deps.toolsreader.v1v3v2.clojure.tools.reader.impl.commons]
;;     #namespace[clojure.spec.gen.alpha]
;;     #namespace[cider.nrepl.inlined-deps.toolsnamespace.v0v3v1.clojure.tools.namespace.track]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.core]
;;     #namespace[clojure.uuid]
;;     #namespace[bultitude.core]
;;     #namespace[complete.core]
;;     #namespace[clojure.main]
;;     #namespace[cider.nrepl.middleware.util.cljs]
;;     #namespace[user]
;;     #namespace[dynapath.dynamic-classpath]
;;     #namespace[clojure.data]
;;     #namespace[cider.nrepl.inlined-deps.suitable.v0v2v14.suitable.js-completions]
;;     #namespace[cider.nrepl.middleware.util]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.clojuredocs]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources.ns-mappings]
;;     #namespace[clojure.edn]
;;     #namespace[pjstadig.humane-test-output]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.context]
;;     #namespace[cemerick.pomegranate]
;;     #namespace[clojure.java.io]
;;     #namespace[cider.nrepl]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.java.resource]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.eldoc]
;;     #namespace[clojure.core.protocols]
;;     #namespace[clojure.pprint]
;;     #namespace[cider.nrepl.inlined-deps.cljs-tooling.v0v3v1.cljs-tooling.util.misc]
;;     #namespace[cider.nrepl.version]
;;     #namespace[nrepl.bencode]
;;     #namespace[nrepl.middleware.load-file]
;;     #namespace[nrepl.version]
;;     #namespace[clojure.instant]
;;     #namespace[ch8joc]
;;     #namespace[cider.nrepl.inlined-deps.toolsreader.v1v3v2.clojure.tools.reader.impl.errors]
;;     #namespace[leiningen.core.utils]
;;     #namespace[cider.nrepl.middleware.classpath]
;;     #namespace[nrepl.transport]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources.namespaces-and-classes]
;;     #namespace[cider.nrepl.middleware.util.meta]
;;     #namespace[cider.nrepl.middleware.out]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.cljs.meta]
;;     #namespace[cider.nrepl.inlined-deps.cljs-tooling.v0v3v1.cljs-tooling.complete]
;;     #namespace[cider.nrepl.inlined-deps.cljs-tooling.v0v3v1.cljs-tooling.util.special]
;;     #namespace[cider.nrepl.inlined-deps.orchard.v0v5v5.orchard.misc]
;;     #namespace[cider.nrepl.middleware.info]
;;     #namespace[cider.nrepl.inlined-deps.suitable.v0v2v14.suitable.complete-for-nrepl]
;;     #namespace[clojure.datafy]
;;     #namespace[cider.nrepl.print-method]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources.keywords]
;;     #namespace[cider.nrepl.inlined-deps.toolsnamespace.v0v3v1.clojure.tools.namespace.find]
;;     #namespace[leiningen.core.main]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.utils]
;;     #namespace[leiningen.core.user]
;;     #namespace[clojure.java.shell]
;;     #namespace[nrepl.core]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources.local-bindings]
;;     #namespace[nrepl.config]
;;     #namespace[cider.nrepl.inlined-deps.compliment.v0v3v9.compliment.sources.class-members]
;;     #namespace[nrepl.middleware.print]
;;     #namespace[clojure.zip]
;;     #namespace[cider.nrepl.middleware.slurp]
;;     #namespace[cider.nrepl.middleware.util.instrument])


