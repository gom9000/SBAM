; La CPU è von Neumann: legge queste istruzioni dal bus, dalla ROM, come legge i dati dalla RAM.
; Conta in RAM, legge gli interruttori, e una subroutine mostra sui LED la somma dei due.

COUNT   EQU 0x8000      ; il contatore, in RAM (8000-BFFF)
SWITCH  EQU 0x8001      ; gli interruttori, copiati in RAM
IO      EQU 0xC000      ; leggere: gli interruttori (244); scrivere: i LED (574)
STACK   EQU 0xC000      ; lo stack in cima alla RAM: il primo byte va a BFFF

        ORG 0x0000      ; la CPU parte da qui all'accensione
reset:  LSP STACK
        LDI 0
        STA COUNT
loop:   LDA IO          ; gli interruttori
        STA SWITCH
        LDA COUNT
        INC
        STA COUNT
        CALL show
        JMP loop

; mostra sui LED A + interruttori; A resta com'era
show:   PUSH
        ADD SWITCH
        STA IO          ; LED = contatore + interruttori
        POP
        RET
