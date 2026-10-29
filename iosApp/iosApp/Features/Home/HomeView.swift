import SwiftUI
import Shared

struct HomeView: View {
    @StateObject private var viewModel: HomeViewModel
    private let navigate: (Route) -> Void

    init(dependencies: AppDependencies, navigate: @escaping (Route) -> Void) {
        _viewModel = StateObject(wrappedValue: HomeViewModel(store: dependencies.container.homeStore()))
        self.navigate = navigate
    }

    var body: some View {
        content
            .navigationTitle("প্রশ্নব্যাংক")
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button { navigate(.search(viewModel.state.selectedExam)) } label: { Image(systemName: "magnifyingglass") }
                        .accessibilityLabel("প্রশ্ন খুঁজুন")
                        .accessibilityIdentifier("home.search")
                    Button { navigate(.bookmarks) } label: { Image(systemName: "bookmark") }
                        .accessibilityLabel("সংরক্ষিত প্রশ্ন")
                        .accessibilityIdentifier("home.bookmarks")
                    Button { navigate(.stats) } label: { Image(systemName: "chart.bar.xaxis") }
                        .accessibilityLabel("পরিসংখ্যান")
                        .accessibilityIdentifier("home.stats")
                }
            }
    }

    @ViewBuilder
    private var content: some View {
        let state = viewModel.state
        if state.isLoading && !state.hasContent {
            ProgressView("প্রশ্নব্যাংক প্রস্তুত হচ্ছে…")
        } else if let message = state.errorMessage, !state.hasContent {
            EmptyStateView(
                systemImage: "exclamationmark.icloud",
                title: "প্রশ্নব্যাংক খোলা যায়নি",
                message: message,
                actionTitle: "আবার চেষ্টা করুন",
                action: viewModel.retry
            )
        } else {
            List {
                Section {
                    Picker("পরীক্ষা", selection: Binding(get: { state.selectedExam }, set: viewModel.selectExam)) {
                        ForEach(state.exams, id: \.self) { exam in
                            Text(Labels.shared.exam(type: exam)).tag(exam)
                        }
                    }
                    .pickerStyle(.segmented)
                    .accessibilityIdentifier("home.exam")
                    .listRowBackground(Color.clear)
                    .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))

                    if state.isSample {
                        Label("নমুনা প্রশ্ন: এগুলো অনুশীলনের জন্য তৈরি, আসল বোর্ড প্রশ্ন নয়।", systemImage: "info.circle")
                            .font(.footnote)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("বোর্ড প্রশ্ন খুঁজুন") {
                    PaperFinder(state: state, viewModel: viewModel) { key in navigate(.paper(PaperRef(key))) }
                }

                Section {
                    SummaryRow(state: state)
                }

                ForEach(state.subjectSections, id: \.title) { section in
                    Section(section.title) {
                        ForEach(section.subjects, id: \.id) { subject in
                            Button { navigate(.subject(subject.id)) } label: {
                                SubjectRow(subject: subject)
                            }
                            .buttonStyle(PressableButtonStyle())
                            .accessibilityIdentifier("home.subject.\(subject.id)")
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .animation(.default, value: state.selectedExam)
        }
    }
}

/// Subject, board and year pickers that open a single board paper.
private struct PaperFinder: View {
    let state: HomeState
    let viewModel: HomeViewModel
    let open: (PaperKey) -> Void

    var body: some View {
        Picker("বিষয়", selection: Binding(get: { state.finderSubjectId ?? "" }, set: viewModel.selectSubject)) {
            ForEach(state.subjects, id: \.id) { Text($0.name).tag($0.id) }
        }
        .accessibilityIdentifier("finder.subject")

        Picker("বোর্ড", selection: Binding(get: { state.finderBoardId ?? "" }, set: viewModel.selectBoard)) {
            ForEach(state.boards, id: \.id) { Text($0.name).tag($0.id) }
        }
        .accessibilityIdentifier("finder.board")

        Picker("সাল", selection: Binding(get: { state.finderYear?.int32Value ?? 0 }, set: viewModel.selectYear)) {
            ForEach(state.years.map(\.int32Value), id: \.self) { Text(Labels.shared.number(value: $0)).tag($0) }
        }
        .accessibilityIdentifier("finder.year")

        if let note = state.finderNote {
            Text(note).font(.footnote).foregroundStyle(.red)
        }

        Button {
            if let key = state.finderKey { open(key) }
        } label: {
            Text("প্রশ্ন দেখুন").font(.headline).frame(maxWidth: .infinity).padding(.vertical, 4)
        }
        .buttonStyle(.borderedProminent)
        .disabled(state.finderKey == nil)
        .accessibilityIdentifier("finder.open")
    }
}

private struct SummaryRow: View {
    let state: HomeState

    var body: some View {
        HStack {
            value(Labels.shared.number(value: state.totalQuestions), "প্রশ্ন")
            value(Labels.shared.number(value: Int32(state.boards.count)), "বোর্ড")
            value(Labels.shared.number(value: state.stats.totalAnswered), "উত্তর দিয়েছেন")
            value(Labels.shared.percent(value: state.stats.accuracyPercent), "সঠিকতা")
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
    }

    private func value(_ value: String, _ label: String) -> some View {
        VStack(spacing: 2) {
            Text(value).font(.title3.weight(.bold)).foregroundStyle(.tint).contentTransition(.numericText())
            Text(label).font(.caption2).foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
}

private struct SubjectRow: View {
    let subject: Shared.Subject

    var body: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 3) {
                Text(subject.name).font(.headline)
                Text("বিষয় কোড \(Labels.shared.digits(text: subject.code)) · \(Labels.shared.number(value: Int32(subject.chapters.count)))টি অধ্যায়")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                Text("\(Labels.shared.number(value: subject.questionCount))টি প্রশ্ন")
                    .font(.caption)
                    .foregroundStyle(.tint)
            }
            Spacer()
            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
        }
        .contentShape(Rectangle())
        .padding(.vertical, 4)
    }
}
