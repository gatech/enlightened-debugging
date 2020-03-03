#!/usr/bin/perl

use strict;
use warnings;

system("jar cvfm iagent.jar src-instr/resources/MANIFEST.MF -C bin instr/agent -C bin instr/transformers -C bin anonymous/domain/enlighten/ExtProperties.class -C bin anonymous/domain/enlighten/data/MethodName.class -C bin anonymous/domain/enlighten/data/SourceLocation.class");
system("jar cvf callback.jar -C bin instr/callback/InstrumentationCallback.class -C bin instr/callback/InstrumentationCallbackListener.class -C bin instr/callback/CallbackDelegation.class -C bin instr/callback/ArrayCopyParams.class");
