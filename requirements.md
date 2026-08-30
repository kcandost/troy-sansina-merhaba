# RozyLab | Game Motion Flow

## 1. Welcome screen / Idle

The screen waits on this scene continuously. The Troy patterns in the background may have a very subtle movement; the word "merhaba" (hello) and the "Hemen Dene" (Try Now) CTA draw attention with a soft, breathing-like scale/pulse animation. When the user taps the button — accompanied by the robot's voice invitation — the flow moves to the next scene.

## 2. Card selection screen

After "Hemen Dene", the "Bir dokunuşla şansını keşfet!" (Discover your luck with a single touch!) screen opens. The 4 cards enter the screen one after another with a small fade/slide and hold briefly. The cards may have subtle hover/shine movements; it is important to give the user the feeling that a selection can be made.

## 3. Shuffle / game start

When the user taps one of the cards, the cards shuffle by swapping positions with each other — fast, but still followable. This movement should last roughly 2–3 seconds. When the shuffle completes, all cards may merge into a single card in the center, or the selected card may move to the center while the others fade out.

## 4. "Hemen çevir, avantajını gör!" (Flip now, see your advantage!) screen

When a single card remains in the center, the headline appears. There may be a subtle glow/pulse around the card. When the user taps the card, it flips on the Y axis with a flip animation and the reward is revealed. The card's rotation can take roughly 0.5–0.8 s; at the moment of the flip, a small light burst / the start of confetti can be used.

## 5. Reward result

After the card is opened, the screen matching the reward amount appears: 250 / 500 / 750 / 1,000 TL. The amount appears large with a scale-up, followed by the "Avantajına merhaba" (Say hello to your advantage) text. The confetti in the background moves briefly, then calms down. The QR code and the instruction text can fade in with a 0.5 s delay.

## 6. Waiting on the QR screen

This screen must stay static so the user can scan the QR; it is healthier to have no movement on the QR itself. Only the area around it may be highlighted with a very subtle pulse/frame animation. After the user has been given enough time, the system automatically returns to the start screen.

## General motion language

The overall language of the movements should be fast, clean and premium; it should not drift too far into a game hall / arcade feel. In keeping with the Troy world, soft easing, subtle glow, scale, fade and card flip movements can be used. Let's use confetti only at the reward moment and keep the other screens simpler.

Suggested total experience: roughly 6–8 seconds of motion excluding user actions; the QR screen can stay open for about 15–20 seconds. Afterwards, a short fade back to the first screen is sufficient.
