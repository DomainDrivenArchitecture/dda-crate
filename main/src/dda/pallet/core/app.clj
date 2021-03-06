; Licensed to the Apache Software Foundation (ASF) under one
; or more contributor license agreements. See the NOTICE file
; distributed with this work for additional information
; regarding copyright ownership. The ASF licenses this file
; to you under the Apache License, Version 2.0 (the
; "License"); you may not use this file except in compliance
; with the License. You may obtain a copy of the License at
;
; http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
(ns dda.pallet.core.app
  (:require
   [schema.core :as s]
   [clojure.tools.logging :as logging]
   [pallet.api :as api]
   [dda.pallet.commons.existing :as existing]
   [dda.pallet.commons.aws :as aws]
   [dda.pallet.commons.external-config :as ext-config]
   [dda.pallet.commons.operation :as operation]
   [dda.pallet.commons.secret :as secret]
   [dda.pallet.core.summary :as summary]))

(def ProvisioningUser existing/ProvisioningUser)

(s/defrecord DdaCrateApp
  [facility :- s/Keyword
   convention-schema :- {s/Any s/Any}
   convention-schema-resolved :- {s/Any s/Any}
   default-convention-file :- s/Str
   default-targets-file :- s/Str]
  Object
  (toString [_] (str "DdaCrateApp[facility=" (:facility _) "]")))

(s/defn dispatch-by-crate-facility :- s/Keyword
  "Dispatcher for phase multimethods by facility. Also does a
   schema validation of arguments."
  [crate-app :- DdaCrateApp
   convention-config]
  (:facility crate-app))

(defmulti group-spec
  "Multimethod for creating group spec."
  dispatch-by-crate-facility)
(s/defmethod group-spec :default
  [crate-app  :- DdaCrateApp
   convention-config]
  (logging/info
    (str crate-app) ": there is no group spec."))

(defmulti load-convention-hook
  "Multimethod to modify convention after load."
  dispatch-by-crate-facility)
(s/defmethod load-convention-hook :default
  [crate-app  :- DdaCrateApp
   convention-config]
  convention-config)

(defmulti load-existing-targets-hook
  "Multimethod to modify convention after load."
  dispatch-by-crate-facility)
(s/defmethod load-existing-targets-hook :default
  [crate-app  :- DdaCrateApp
   targets]
  targets)

(defprotocol Domain
  (load-convention
    [crate-app file-name]
    "load the convention from classpath or filesystem."))

(defprotocol SessionSummarization
  (summarize-test-session [crate-app session & options]
    "make summary of tests session")
  (test-session-passed? [crate-app session]
    "inspect session whether all tests has passed.")
  (default-session-passed? [crate-app session]
    "inspect session whether all tests has passed."))

(defprotocol ExistingTargets
  "Protocol for interact on existing targets"
  (load-existing-targets
    [crate-app file-name]
    "load targets configuration from classpath / filesystem.")
  (existing-provider-resolved
    [crate-app targets-config]
    "the existing provider for allready resolved configurations.")
  (existing-provider
    [crate-app targets-config]
    "the existing provider for unresolved configuration")
  (existing-provisioning-spec-resolved [crate-app convention-config targets-config])
  (existing-provisioning-spec [crate-app convention-config targets-config])
  (execute-existing-serverspec [crate-app convention-config target-config verbosity])
  (execute-existing-install [crate-app convention-config target-config])
  (execute-existing-configure [crate-app convention-config target-config])
  (execute-existing-app-rollout [crate-app convention-config target-config]))

(defprotocol AwsTargets
  "Protocol for interact on existing targets"
  (load-aws-targets
    [crate-app file-name]
    "load targets configuration from classpath / filesystem.")
  (aws-provider-resolved
    [crate-app targets-config]
    "the existing provider for allready resolved configurations.")
  (aws-provider
    [crate-app targets-config]
    "the existing provider for unresolved configuration")
  (aws-provisioning-spec-resolved [crate-app convention-config targets-config count])
  (aws-provisioning-spec [crate-app convention-config targets-config count])
  (execute-aws-serverspec [crate-app convention-config target-config verbosity])
  (execute-aws-install [crate-app convention-config target-config count])
  (execute-aws-configure [crate-app convention-config target-config]))

(defprotocol ExistingIntegration
  (existing-install [crate-app options])
  (existing-configure [crate-app options])
  (existing-serverspec [crate-app options])
  (existing-app-rollout [crate-app options]))

(defprotocol AwsIntegration
  (aws-install [crate-app count options])
  (aws-configure [crate-app options])
  (aws-serverspec [crate-app options]))

(extend-type DdaCrateApp

  Domain
  (load-convention [crate-app file-name]
    (s/validate s/Str file-name)
    (s/validate (:convention-schema crate-app)
                (load-convention-hook crate-app
                                  (ext-config/parse-config file-name))))

  SessionSummarization
    ; TODO: validate as soon as pallet-commons issue is fixed
    ;[session :- session/SessionSpec
  (summarize-test-session [crate-app session & options]
    (apply summary/summarize-test-session (cons session options)))
    ; TODO: validate as soon as pallet-commons issue is fixed
    ;[session :- session/SessionSpec]
  (test-session-passed? [crate-app session]
    (let [result (apply summary/test-session-passed? '(session))]
      (s/validate s/Bool result)))
  (default-session-passed? [crate-app session]
    (let [result (apply summary/default-session-passed? '(session))]
      (s/validate s/Bool result)))

  ExistingTargets
  (load-existing-targets [crate-app file-name]
    (s/validate s/Str file-name)
    (s/validate existing/Targets
                (load-existing-targets-hook crate-app
                                            (existing/load-targets file-name))))
  (existing-provider-resolved [crate-app targets-config]
    (s/validate existing/TargetsResolved targets-config)
    (let [{:keys [existing]} targets-config]
      (existing/provider {(:facility crate-app) existing})))
  (existing-provider [crate-app targets-config]
    (s/validate existing/Targets targets-config)
    (existing-provider-resolved
     crate-app
     (existing/resolve-targets targets-config)))
  (existing-provisioning-spec-resolved
    [crate-app convention-config targets-config]
    (s/validate (:convention-schema-resolved crate-app) convention-config)
    (s/validate existing/TargetsResolved targets-config)
    (let [{:keys [existing provisioning-user]} targets-config]
      (merge
       (group-spec crate-app convention-config)
       (existing/node-spec provisioning-user))))
  (existing-provisioning-spec
    [crate-app convention-config targets-config]
    (s/validate (:convention-schema crate-app) convention-config)
    (s/validate existing/Targets targets-config)
    (existing-provisioning-spec-resolved
     crate-app
     (secret/resolve-secrets convention-config (:convention-schema crate-app))
     (existing/resolve-targets targets-config)))
  (execute-existing-serverspec [crate-app convention-config target-config verbosity]
    (let [session (operation/do-test
                   (existing-provider crate-app target-config)
                   (existing-provisioning-spec crate-app convention-config target-config)
                   :summarize-session false)]
      (summary/summarize-test-session session :verbose verbosity)
      (summary/test-session-passed? session)))
  (execute-existing-app-rollout [crate-app convention-config target-config]
    (let [session (operation/do-app-rollout
                   (existing-provider crate-app target-config)
                   (existing-provisioning-spec crate-app convention-config target-config)
                   :summarize-session true)]
      (summary/default-session-passed? session)))
  (execute-existing-install [crate-app convention-config target-config]
    (let [session (operation/do-apply-install
                   (existing-provider crate-app target-config)
                   (existing-provisioning-spec crate-app convention-config target-config)
                   :summarize-session true)]
      (summary/default-session-passed? session)))
  (execute-existing-configure [crate-app convention-config target-config]
    (let [session (operation/do-apply-configure
                   (existing-provider crate-app target-config)
                   (existing-provisioning-spec crate-app convention-config target-config)
                   :summarize-session true)]
      (summary/default-session-passed? session)))

  AwsTargets
  (load-aws-targets [crate-app file-name]
    (s/validate s/Str file-name)
    (s/validate aws/Targets
                (aws/load-targets file-name)))
  (aws-provider-resolved [crate-app targets-config]
    (s/validate aws/TargetsResolved targets-config)
    (let [{:keys [context]} targets-config]
      (aws/provider context)))
  (aws-provider [crate-app targets-config]
    (s/validate aws/Targets targets-config)
    (aws-provider-resolved
     crate-app
     (aws/resolve-targets targets-config)))
  (aws-provisioning-spec-resolved
    [crate-app convention-config targets-config count]
    (s/validate (:convention-schema-resolved crate-app) convention-config)
    (s/validate aws/TargetsResolved targets-config)
    (s/validate s/Num count)
    (merge
     (group-spec crate-app convention-config)
     (aws/node-spec (:node-spec targets-config))
     {:count count}))
  (aws-provisioning-spec
    [crate-app convention-config targets-config count]
    (s/validate (:convention-schema crate-app) convention-config)
    (s/validate aws/Targets targets-config)
    (s/validate s/Num count)
    (aws-provisioning-spec-resolved
     crate-app
     (secret/resolve-secrets convention-config (:convention-schema crate-app))
     (aws/resolve-targets targets-config)
     count))
  (execute-aws-serverspec [crate-app convention-config target-config verbosity]
    (let [session (operation/do-test
                   (aws-provider crate-app target-config)
                   (aws-provisioning-spec crate-app convention-config target-config 0)
                   :summarize-session false)]
      (summary/summarize-test-session session :verbose verbosity)
      (summary/test-session-passed? session)))
  (execute-aws-install [crate-app convention-config target-config count]
    (let [session (operation/do-converge-install
                   (aws-provider crate-app target-config)
                   (aws-provisioning-spec crate-app convention-config target-config count)
                   :summarize-session true)]
      (summary/default-session-passed? session)))
  (execute-aws-configure [crate-app convention-config target-config]
    (let [session (operation/do-apply-configure
                   (aws-provider crate-app target-config)
                   (aws-provisioning-spec crate-app convention-config target-config 0)
                   :summarize-session true)]
      (summary/default-session-passed? session)))

  ExistingIntegration
  (existing-install [crate-app options]
    (let [{:keys [convention targets]} options
          target-config (if (some? targets)
                          (load-existing-targets crate-app targets)
                          (load-existing-targets crate-app (:default-targets-file crate-app)))
          convention-config (if (some? convention)
                          (load-convention crate-app convention)
                          (load-convention crate-app (:default-convention-file crate-app)))]
      (execute-existing-install crate-app convention-config target-config)))
  (existing-configure [crate-app options]
    (let [{:keys [convention targets]} options
          target-config (if (some? targets)
                          (load-existing-targets crate-app targets)
                          (load-existing-targets crate-app (:default-targets-file crate-app)))
          convention-config (if (some? convention)
                          (load-convention crate-app convention)
                          (load-convention crate-app (:default-convention-file crate-app)))]
      (execute-existing-configure crate-app convention-config target-config)))
  (existing-serverspec [crate-app options]
    (let [{:keys [convention targets verbosity]
           :or {verbosity 1}} options
          target-config (if (some? targets)
                          (load-existing-targets crate-app targets)
                          (load-existing-targets crate-app (:default-targets-file crate-app)))
          convention-config (if (some? convention)
                          (load-convention crate-app convention)
                          (load-convention crate-app (:default-convention-file crate-app)))]
      (execute-existing-serverspec crate-app convention-config target-config verbosity)))
  (existing-app-rollout [crate-app options]
    (let [{:keys [convention targets]} options
          target-config (if (some? targets)
                          (load-existing-targets crate-app targets)
                          (load-existing-targets crate-app (:default-targets-file crate-app)))
          convention-config (if (some? convention)
                          (load-convention crate-app convention)
                          (load-convention crate-app (:default-convention-file crate-app)))]
      (execute-existing-app-rollout crate-app convention-config target-config)))

  AwsIntegration
  (aws-install [crate-app count options]
    (let [{:keys [convention targets]} options
          target-config (load-aws-targets crate-app targets)
          convention-config (if (some? convention)
                          (load-convention crate-app convention)
                          (load-convention crate-app (:default-convention-file crate-app)))]
      (execute-aws-install crate-app convention-config target-config count)))
  (aws-configure [crate-app options]
    (let [{:keys [convention targets]} options
          target-config (if (some? targets)
                          (load-aws-targets crate-app targets)
                          (load-aws-targets crate-app (:default-targets-file crate-app)))
          convention-config (if (some? convention)
                          (load-convention crate-app convention)
                          (load-convention crate-app (:default-convention-file crate-app)))]
      (execute-aws-configure crate-app convention-config target-config)))
  (aws-serverspec [crate-app options]
    (let [{:keys [convention targets]} options
          target-config (if (some? targets)
                          (load-aws-targets crate-app targets)
                          (load-aws-targets crate-app (:default-targets-file crate-app)))
          convention-config (if (some? convention)
                          (load-convention crate-app convention)
                          (load-convention crate-app (:default-convention-file crate-app)))]
      (execute-aws-serverspec crate-app convention-config target-config 1))))


(defn make-dda-crate-app
  "Creates a DdaCrateApp. (Wrapper for ->DdaCrateApp with validation.)"
  [& {:keys [facility convention-schema convention-schema-resolved
             default-convention-file default-targets-file]
      :or {default-targets-file "targets.edn"}}]
  (s/validate
    DdaCrateApp
    (->DdaCrateApp facility convention-schema convention-schema-resolved
                   default-convention-file default-targets-file)))

(defn pallet-group-spec [app-config server-specs]
 (let [group-name (name (key (first (:group-specific-config app-config))))]
   (api/group-spec
    group-name
    :extends server-specs)))
