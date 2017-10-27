# dda-pallet
[![Clojars Project](https://img.shields.io/clojars/v/dda/dda-pallet.svg)](https://clojars.org/dda/dda-pallet)
[![Build Status](https://travis-ci.org/DomainDrivenArchitecture/dda-pallet.svg?branch=master)](https://travis-ci.org/DomainDrivenArchitecture/dda-pallet)

[Meet us on Slack](http://clojurians.herokuapp.com/) | [Meet us on Meetup](https://www.meetup.com/de-DE/preview/dda-pallet-DevOps-Hacking-with-Clojure) | [Website & Blog](https://domaindrivenarchitecture.org)

Anyone who has so far dealt with Infrastructure as Code has to deal with a mix of Ruby, Python and go. If you want to get more from the Java world and always wanted to try functional programming, there is now a worthy & fresh alternative: "dda-pallet". Dda-pallet is OpenSource, written in clojure, running on the proven Java Virtual Machine.
dda-pallet provides the whole tool-chain from one hand, is real modular, testable and well prepared for configuration data handling.

## compatability
dda-pallet is compatible to the following versions
 * pallet 0.8
 * clojure 1.7
 * (x)ubuntu14.04 / 16.04

## Principles
 * Distinction between installation and configuration: We distinguish between seldom installation and continuous configuration.
 * Explicit State: We collect all the system state information to a defined place.
 * Full Modularization: We separate our modules into common usable system adapters and company specific convention modules. Beside this general principle we are using the full tool stack provided by clojure.
 * Configuration is data: In order to keep the interface simple, clean & isolated, we handle all configuration as data. There is no magic hidden in.
 * Test Driven DevOps: Configuration as data makes it easy, to do unit tests. For the system adapters we are heading towards to state of the art integration tests - comparable to kitchen / vagrant / server-spec.

## TechDebt Roadmap

Find closer description to the DesignDecisions outlined here: https://dda.gitbooks.io/domaindrivenarchitecture/content/v/da2b436b8932b968b0a4514e0fa5f82e52b5c82c/en/80_devops/40_architecture/Decisions.html

| | Version | Separate Domain from Infrastructure | Integration Folder | Docker based Integration Tests | Unit Tests for Domain | Boundaries | Input / Output Spec | Short Package | Composition over API | Group-based Configuration | Use dda-pallet aws/existing | Use app layer | DDD ns layout | CI |
| --- | --- |  --- |--- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| dda-backup-crate | 0.5.0-SNAPSHOT |  |  |  | | x |  |  | |  |  |  |  | x |
| dda-config-commons| 0.2.2 | n.a. | n.a. | n.a. |  | x | x | x | n.a | n.a | n.a. | n.a. | n.a. | x |
| dda-git-crate   | 0.1.2 | x | x |  | x | x | x | x | x | x | x | x | x | x |
| dda-hardening-crate| 0.3.0-SNAPSHOT | x | x |  |  | x | x | x | x | x | x |  | x | x |
| dda-httpd-crate| 0.2.2-SNAPSHOT | x | x | x |  | x | x | x | x | x | x | x | x | x |
| dda-liferay-crate|  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| dda-managed-ide| 0.1.3-SNAPSHOT | x | x |  | x | x |  | x | x | x |  |  |  |  |
| dda-managed-vm      | 0.3.0 | x | x | x |  | x | x | x | x | x | x | x | x | x |
| dda-mariadb-crate   | 0.2.0-SNAPSHOT | x | x |  |  | x | x | x | x | x | x | x | x | x |
| dda-pallet          | 0.5.3 | x | n.a. |  | x |  | x |  | x | x | x | x | x | x |
| dda-pallet-commons  | 0.4.1 | n.a. | n.a |  |  |  |  | x |  | n.a. | n.a | n.a. | n.a. | x |
| dda-provider-crate  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |  |
| dda-serverspec-crate| 0.5.3 | x | x |  | x | x | x | x | x | x | x | x | x | x |
| dda-tomcat-crate| 0.2.0-SNAPSHOT | x | x |  | x  | x | x | x | x | x | x | x | x | x |
| dda-user-crate| 0.6.0 | x | x |  | x | x | x | x | x | x | x | x | x | x |
| dda-init-crate| deprecated |  |  |  | |  |  |  | ||  |  |  |  |
| dda-linkeddata-crate| deprecated |  |  |  | |  |  |  || |  |  |  |  |
| dda-basic-crate | deprecated |  |  |  | |  |  |  | ||  |  |  |  |
| dda-config-crate| deprecated |  |  |  |  |  |  |  |  |  |  |  |  |  |
| dda-collected-crate| deprecated |  |  |  | |  |  |  |  |  |  |  |  |  |
| dda-iptables-crate| deprecated |  |  |  |  |  |  | x |  |  |  |  |  |  |

# License
Published under [apache2.0 license](LICENSE.md)
