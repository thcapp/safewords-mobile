import SwiftUI

struct ChallengeSheet: View {
    @Environment(\.dismiss) private var dismiss

    let group: Group
    let seed: Data

    @State private var rowIndex = 0
    @State private var showTable = false
    @State private var authFailed = false

    private var row: ChallengeAnswerRow {
        Primitives.challengeAnswerRow(
            seed: seed,
            tableVersion: group.primitives.challengeAnswer.tableVersion,
            rowIndex: rowIndex
        )
    }

    private var rowCount: Int {
        max(1, group.primitives.challengeAnswer.rowCount)
    }

    var body: some View {
        ZStack {
            Ink.bg.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 18) {
                    Capsule()
                        .fill(Ink.rule)
                        .frame(width: 42, height: 4)
                        .frame(maxWidth: .infinity)

                    SectionLabel(text: "Challenge someone")
                    Text(group.name)
                        .font(Fonts.display(30))
                        .tracking(-0.9)
                        .foregroundStyle(Ink.fg)

                    stepperCard
                    promptCard

                    if authFailed {
                        Text("Unlock failed. Try again before showing the table.")
                            .font(Fonts.body(12.5))
                            .foregroundStyle(Ink.warn)
                    }

                    HStack(spacing: 10) {
                        Button("They said \(row.expect.phrase)") {
                            dismiss()
                        }
                        .font(Fonts.body(13.5, weight: .semibold))
                        .foregroundStyle(Ink.accentInk)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(Capsule().fill(Ink.accent))

                        Button("Does not match") {
                            dismiss()
                        }
                        .font(Fonts.body(13.5, weight: .semibold))
                        .foregroundStyle(Ink.fg)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 13)
                        .background(Capsule().stroke(Ink.rule, lineWidth: 0.5))
                    }

                    Button {
                        Task { await revealTable() }
                    } label: {
                        HStack {
                            Image(systemName: "lock")
                            Text(showTable ? "Hide full table" : "Show full table")
                            Spacer()
                        }
                        .font(Fonts.body(14, weight: .semibold))
                        .foregroundStyle(Ink.fg)
                        .padding(14)
                        .background(RoundedRectangle(cornerRadius: 16).fill(Ink.bgElev))
                    }
                    .buttonStyle(.plain)

                    if showTable {
                        tableRows
                    }
                }
                .padding(20)
            }
            .scrollIndicators(.hidden)
        }
    }

    private var stepperCard: some View {
        HStack(spacing: 12) {
            Button { rowIndex = max(0, rowIndex - 1) } label: {
                Image(systemName: "minus")
                    .frame(width: 38, height: 38)
                    .background(Circle().fill(Ink.bgInset))
            }
            .buttonStyle(.plain)

            VStack(spacing: 2) {
                Text("Row \(rowIndex + 1) of \(rowCount)")
                    .font(Fonts.body(16, weight: .semibold))
                    .foregroundStyle(Ink.fg)
                Text("Pick any row. Both sides derive the same table.")
                    .font(Fonts.body(11.5))
                    .foregroundStyle(Ink.fgMuted)
            }
            .frame(maxWidth: .infinity)

            Button { rowIndex = min(rowCount - 1, rowIndex + 1) } label: {
                Image(systemName: "plus")
                    .frame(width: 38, height: 38)
                    .background(Circle().fill(Ink.bgInset))
            }
            .buttonStyle(.plain)
        }
        .foregroundStyle(Ink.fg)
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 18).fill(Ink.bgElev))
    }

    private var promptCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            phraseBlock(label: "Ask", phrase: row.ask.phrase)
            Rectangle().fill(Ink.rule).frame(height: 0.5)
            phraseBlock(label: "Expect", phrase: row.expect.phrase)
        }
        .padding(18)
        .background(
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .fill(Ink.bgElev)
                .overlay(RoundedRectangle(cornerRadius: 22).stroke(Ink.rule, lineWidth: 0.5))
        )
    }

    private func phraseBlock(label: String, phrase: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            SectionLabel(text: label)
            Text(phrase)
                .font(Fonts.display(34))
                .tracking(-0.9)
                .foregroundStyle(label == "Ask" ? Ink.fg : Ink.accent)
        }
    }

    private var tableRows: some View {
        VStack(spacing: 0) {
            ForEach(0..<rowCount, id: \.self) { index in
                let tableRow = Primitives.challengeAnswerRow(
                    seed: seed,
                    tableVersion: group.primitives.challengeAnswer.tableVersion,
                    rowIndex: index
                )
                HStack(alignment: .top, spacing: 10) {
                    Text("\(index + 1)")
                        .font(Fonts.mono(10))
                        .foregroundStyle(Ink.fgFaint)
                        .frame(width: 24, alignment: .leading)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(tableRow.ask.phrase)
                        Text(tableRow.expect.phrase)
                            .foregroundStyle(Ink.accent)
                    }
                    .font(Fonts.body(12.5, weight: .medium))
                    Spacer()
                }
                .foregroundStyle(Ink.fg)
                .padding(.vertical, 8)
                if index < rowCount - 1 {
                    Rectangle().fill(Ink.rule).frame(height: 0.5)
                }
            }
        }
        .padding(14)
        .background(RoundedRectangle(cornerRadius: 18).fill(Ink.bgElev))
    }

    @MainActor
    private func revealTable() async {
        if showTable {
            showTable = false
            return
        }
        let ok = await BiometricService.authenticateDeviceOwner(reason: "Show challenge answer table")
        authFailed = !ok
        showTable = ok
    }
}
