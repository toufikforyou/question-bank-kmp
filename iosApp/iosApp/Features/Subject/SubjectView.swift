import SwiftUI
import Shared

struct SubjectView: View {
    @StateObject private var viewModel: SubjectViewModel
    private let navigate: (Route) -> Void

    init(store: SubjectStore, navigate: @escaping (Route) -> Void) {
        _viewModel = StateObject(wrappedValue: SubjectViewModel(store: store))
        self.navigate = navigate
    }

    var body: some View {
        let state = viewModel.state
        Group {
            if state.isLoading {
                ProgressView()
            } else if let message = state.errorMessage {
                EmptyStateView(systemImage: "exclamationmark.triangle", title: "বিষয়টি খোলা যায়নি", message: message,
                               actionTitle: "আবার চেষ্টা করুন", action: viewModel.retry)
            } else {
                List {
                    Section {
                        Text(state.subtitle).font(.subheadline).foregroundStyle(.secondary)
                        Picker("দেখুন", selection: Binding(get: { state.tab }, set: { viewModel.select(tab: $0) })) {
                            Text("বোর্ড প্রশ্ন").tag(SubjectTab.papers)
                            Text("অধ্যায়ভিত্তিক").tag(SubjectTab.chapters)
                        }
                        .pickerStyle(.segmented)
                        .accessibilityIdentifier("subject.tabs")
                    }
                    if state.tab == .papers {
                        papers(state)
                    } else {
                        chapters(state)
                    }
                }
                .listStyle(.insetGrouped)
                .animation(.default, value: state.tab)
                .animation(.default, value: state.boardFilter)
            }
        }
        .navigationTitle(state.title)
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func papers(_ state: SubjectState) -> some View {
        Section {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    FilterChip(title: "সব বোর্ড", isSelected: state.boardFilter == nil) { viewModel.filter(boardId: nil) }
                    ForEach(state.boards, id: \.id) { board in
                        FilterChip(title: board.name, isSelected: state.boardFilter == board.id) { viewModel.filter(boardId: board.id) }
                            .accessibilityIdentifier("subject.board.\(board.id)")
                    }
                }
                .padding(.vertical, 4)
            }
            .listRowInsets(EdgeInsets(top: 0, leading: 12, bottom: 0, trailing: 12))
        }
        ForEach(state.yearSections, id: \.year) { section in
            Section(section.title) {
                if let note = section.note {
                    Label(note, systemImage: "info.circle").font(.footnote).foregroundStyle(.secondary)
                }
                ForEach(section.papers, id: \.title) { paper in
                    Button { navigate(.paper(PaperRef(paper.key))) } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(paper.title).font(.body.weight(.medium))
                                Text(paper.countLabel).font(.caption).foregroundStyle(.secondary)
                            }
                            Spacer()
                            Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                        }
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(PressableButtonStyle())
                    .accessibilityIdentifier("subject.paper.\(paper.key.boardId).\(paper.key.year)")
                }
            }
        }
    }

    @ViewBuilder
    private func chapters(_ state: SubjectState) -> some View {
        if let request = state.practiceRequest {
            Section {
                Button { navigate(.quiz(request)) } label: {
                    Label("এই বিষয়ের বহুনির্বাচনি অনুশীলন", systemImage: "play.fill")
                }
                .accessibilityIdentifier("subject.practise")
            }
        }
        Section("অধ্যায়") {
            ForEach(state.chapters, id: \.id) { chapter in
                Button { navigate(.chapter(chapter.id)) } label: {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text(state.chapterLabel(chapter: chapter)).font(.body.weight(.medium))
                            Text(state.chapterCountLabel(chapter: chapter)).font(.caption).foregroundStyle(.secondary)
                        }
                        Spacer()
                        Image(systemName: "chevron.right").foregroundStyle(.tertiary)
                    }
                    .contentShape(Rectangle())
                }
                .buttonStyle(PressableButtonStyle())
                .accessibilityIdentifier("subject.chapter.\(chapter.id)")
            }
        }
    }
}

struct FilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline.weight(isSelected ? .semibold : .regular))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Capsule().fill(isSelected ? Color.accentColor.opacity(0.18) : Color(.tertiarySystemFill)))
                .foregroundStyle(isSelected ? Color.accentColor : Color.primary)
        }
        .buttonStyle(PressableButtonStyle())
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}
