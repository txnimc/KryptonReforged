![image](https://user-images.githubusercontent.com/67132971/143149709-8b5044f8-f184-4186-a1dd-c15965f5b0e9.png)

## THIS IS AN UNOFFICIAL FORGE PORT.

 <i>Please direct bug reports, questions, and everything else to the Github page linked to this Curseforge page, not the Fabric mod. The original maintainer, Tuxed, will not be providing any support for this port. </i>

<hr />

Krypton optimizes the Minecraft networking stack, meaning it mostly improves server performance with lots of clients, but is also required on the client. It derives from work done in the Velocity and Tuinity projects. Krypton contains several optimizations, including:

- Highly optimized Netty handlers derived from the Velocity proxy.
- Flush consolidation to lower server CPU usage (and reducing the impact from hardware security vulnerabilities which exploit speculative execution) and lower server tick times.
- Micro-optimizations to reduce memory usage and improve packet serialization speeds.
 

As for this port, it should be compatible with almost everything, as it's just a few mixins. Anything that modifies the networking stack may cause issues, but these patches are low level optimizations and unlikely to cause problems. There were issues however using jar-in-jar on 1.16, so the Velocity natives are packaged using Shade. This also seems to have packaged a few other duplicate Netty classes even with jar minimization and exclusion. It seems to work fine regardless, but if it breaks loading these classes somehow, or if anyone knows how to fix this, let me know.

 
 <p align="center">
  Do not ask for 1.17 port. It is a half finished useless version, I will port to 1.18 after the upstream maintainer does as well.
</p>

 <p align="center">
  <i>In Hoc Signo Vinces</i>
</p>
<p align="center">
  <a href="https://discord.gg/kyBCuYUzFB">Our Discord</a>
</p>



